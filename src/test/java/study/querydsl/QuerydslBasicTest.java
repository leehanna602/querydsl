package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.*;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import java.util.List;

import static com.querydsl.jpa.JPAExpressions.select;
import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {
    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL() {
        // member1을 찾아라
        String qlString =
                "select m from Member m " +
                "where m.username = :username";
        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    /* Querydsl의 장점
    *  - 컴파일 시점 오류 발견 가능 (쿼리)
    *  - 파라미터 바인딩이 자동으로 됨
    */
    @Test
    public void startQuerydsl() {
         /* Q클래스 인스턴스를 사용하는 2가지 방법
         *  1. 별칭 직접 지정
         *  2. 기본 인스턴스 사용 + static (권장)
         *  - 같은 테이블을 조인해야하는 경우가 아니면 기본 인스턴스 사용하기
         *  - 실행되는 JPQL을 보면 alias가 QMember에 기본 설정된 값 사용됨
         *  - select member1 from Member member1 ...
         */
//        QMember m = new QMember("m"); // 별칭 직접 지정
//        QMember m = QMember.member; // 기본 인스턴스 사용 -> static import로 사용하면 코드 더 줄어든다 (권장)
        
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1")) // 파라미터 바인딩이 자동으로 됨 - JPQL과 차이
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search() {
        Member findMember = queryFactory
                .selectFrom(member) // select, from -> selectFrom 가능
                .where(
                        // and(), or() 로 연결
                        member.username.eq("member1").and(member.age.eq(10))
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam() {
        Member findMember = queryFactory
                .selectFrom(member) // select, from -> selectFrom 가능
                .where(
                        // and() 대신 , 가능
                        // 파라미터로 처리할 경우 장점 -> null값은 무시 -> 메서드 추출을 활용해서 동적 쿼리 깔끔하게 만들 수 있음
                        member.username.eq("member1"), (member.age.eq(10))
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchTest() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
//                        member.username.eq("member1")
//                        member.username.ne("member1")
//                        member.username.eq("member1").not()

//                        member.username.isNotNull()

//                        member.age.in(10, 20)
//                        member.age.notIn(10, 20)
//                        member.age.between(10, 30)

//                        member.age.goe(30) // age >= 30
//                        member.age.gt(30) // age > 30
//                        member.age.loe(20)
                        member.age.lt(20)

//                        member.username.like("member%")
//                        member.username.contains("member")
//                        member.username.startsWith("member")
                )
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch() {
        // List 조회 (데이터 없으면 빈 리스트 반환)
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        /* 단 건 조회
        *   - 결과가 없으면 : null
        *   - 결과가 둘 이상이면 : com.querydsl.core.NonUniqueResultException
        */
        Member fetchOne = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        // 처음 한 건 조회 - fetchFirst() => limit(1).fetchOne()
        Member fetchFirst = queryFactory
                .selectFrom(member)
                .fetchFirst();

        // count(*) 쿼리 - 응답 결과는 숫자 하나이므로 fetchOne() 을 사용
        Long totalCount = queryFactory
                .select(Wildcard.count)
                .from(member)
                .fetchOne();
        System.out.println("totalCount = " + totalCount);

        // count(member.id) 쿼리
        Long totalMemberCount = queryFactory
                .select(member.count())
                .from(member)
                .fetchOne();
        System.out.println("totalMemberCount = " + totalMemberCount);
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 3. 단 2에서 회원이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        /* 정렬
        *   - desc() , asc() : 일반 정렬
        *   - nullsLast() , nullsFirst() : null 데이터 순서 부여
        */
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging1() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) // 앞에 몇개를 스킵할지 0부터 시작, 1이면 하나를 스킵
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void aggregation() {
         /* Tuple
         *  - member 단일 타입 조회가 아니라 데이터 타입이 여러개일 때 tuple 사용
         *  - 실무에서는 주로 Tuple 보다는 DTO 사용
         */
        List<Tuple> result = queryFactory
                .select(member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min())
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     */
    @Test
    public void group() throws Exception {
        List<Tuple> result = queryFactory.select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();
        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    /**
     * 팀 A애 소속된 모든 회원
     */
    @Test
    public void join(){
        List<Member> result = queryFactory.selectFrom(member)
                .join(member.team, team) // inner join
                .where(team.name.eq("teamA")) // .on(team.name.eq("teamA")) 과 동일
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2"); // 순서 까지 정확해야함
    }

    /**
     * 세타 조인 (연관관계가 없는 필드로 조인)
     * - cross join
     * - 선택한 엔티티의 모든 데이터를 다 조인한 후, where절로 조건 필터링
     * - from 절에 여러 엔티티를 선택해서 세타 조인.
     * - 이전엔 외부 조인 불가능 -> on을 사용하면 가능.
     * 예제
     * - 회원의 이름이 팀 이름과 같은 회원 조회
     */
    @Test
    public void theta_join(){
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /**
     * ON절을 활용한 조인 1 : 조인 대상 필터링
     * 
     * 예제
     * - 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회 (left join)
     * - JPQL : SELECT  m, t FROM Member m LEFT JOIN m.team t on t.name = 'teamA'
     * - SQL : SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.TEAM_ID=t.id and t.name='teamA'
     * 
     * 결과 예시
     * tuple = [Member(id=3, username=member1, age=10), Team(id=1, name=teamA)]
     * tuple = [Member(id=4, username=member2, age=20), Team(id=1, name=teamA)]
     * tuple = [Member(id=5, username=member3, age=30), null]
     * tuple = [Member(id=6, username=member4, age=40), null]
     */
    @Test
    public void join_on_filtering() {
        List<Tuple> result = queryFactory.select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * ON절을 활용한 조인 2 : 연관관계 없는 엔티티 외부 조인
     *
     * 예제
     * - 회원의 이름과 팀의 이름이 같은 대상 외부 조인
     * - JPQL : SELECT m, t FROM Member m LEFT JOIN Team t on m.username = t.name
     * - SQL : SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.username = t.name
     * 
     * 결과 예시
     * t=[Member(id=3, username=member1, age=10), null]
     * t=[Member(id=4, username=member2, age=20), null]
     * t=[Member(id=5, username=member3, age=30), null]
     * t=[Member(id=6, username=member4, age=40), null]
     * t=[Member(id=7, username=teamA, age=0), Team(id=1, name=teamA)]
     * t=[Member(id=8, username=teamB, age=0), Team(id=2, name=teamB)]
     */
    @Test
    public void join_on_no_relation(){
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name)) // 일반 조인과 다르게 엔티티 하나만 들어감!
                // inner join
                // t=[Member(id=7, username=teamA, age=0), Team(id=1, name=teamA)]
                // t=[Member(id=8, username=teamB, age=0), Team(id=2, name=teamB)]
                // .join(team).on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("t=" + tuple);
        }
    }

    @Test
    public void join_summary_test(){
        // 연관관계 있는 조인

        /* 결과 1
        t=[Member(id=3, username=member1, age=10), Team(id=1, name=teamA)]
        t=[Member(id=4, username=member2, age=20), Team(id=1, name=teamA)]
         */
        // 방법1 (권장)
//        List<Tuple> result = queryFactory.select(member, team)
//                .from(member)
//                .join(member.team, team)
//                .where(team.name.eq("teamA"))
//                .fetch();
        // 방법2
//        List<Tuple> result = queryFactory.select(member, team)
//                .from(member)
//                .join(member.team, team)
//                .on(team.name.eq("teamA"))
//                .fetch();

        /* 결과 2
        t=[Member(id=3, username=member1, age=10), Team(id=1, name=teamA)]
        t=[Member(id=4, username=member2, age=20), Team(id=1, name=teamA)]
        t=[Member(id=5, username=member3, age=30), null]
        t=[Member(id=6, username=member4, age=40), null]
         */
        // 방법1
//        List<Tuple> result = queryFactory.select(member, team)
//                .from(member)
//                .leftJoin(member.team, team)
//                .on(team.name.eq("teamA"))
//                .fetch();

        /* 결과
        t=[Member(id=3, username=member1, age=10), Team(id=1, name=teamA)]
        t=[Member(id=4, username=member2, age=20), Team(id=1, name=teamA)]
        t=[Member(id=5, username=member3, age=30), Team(id=1, name=teamA)]
        t=[Member(id=6, username=member4, age=40), Team(id=1, name=teamA)]
        t=[null, Team(id=2, name=teamB)]
         */
//        List<Tuple> result = queryFactory.select(member, team)
//                .from(member)
//                .rightJoin(team)
//                .on(team.name.eq("teamA"))
//                .fetch();

        
        // 연관관계 없는 조인 (문법이 다름!)
        /* 결과 1
         * t=[Member(id=7, username=teamA, age=0), Team(id=1, name=teamA)]
         * t=[Member(id=8, username=teamB, age=0), Team(id=2, name=teamB)]
         */
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        // 방법1
//        List<Tuple> result = queryFactory.select(member, team)
//                .from(member, team)
//                .where(member.username.eq(team.name))
//                .fetch();
        // 방법2
//        List<Tuple> result = queryFactory.select(member, team)
//                .from(member)
//                .join(team)
//                .on(member.username.eq(team.name))
//                .fetch();

        /* 결과 2
         * t=[Member(id=3, username=member1, age=10), null]
         * t=[Member(id=4, username=member2, age=20), null]
         * t=[Member(id=5, username=member3, age=30), null]
         * t=[Member(id=6, username=member4, age=40), null]
         * t=[Member(id=7, username=teamA, age=0), Team(id=1, name=teamA)]
         * t=[Member(id=8, username=teamB, age=0), Team(id=2, name=teamB)]
         */
        List<Tuple> result = queryFactory.select(member, team)
                .from(member)
                .leftJoin(team)
                .on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("t="+tuple);
        }
    }


    @PersistenceUnit
    EntityManagerFactory emf;

    /*
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "team_id")
        private Team team;

        결과적으로 member만 가져옴
     */
    @Test
    public void fetchJoinNo(){
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    /*
        Team을 함께 조회
        .fetchJoin() 사용
     */
    @Test
    public void fetchJoinUse() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 적용").isTrue();
    }

    /**
     * 나이가 가장 많은 회원 조회
     */
    @Test
    public void subQuery() {

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(40);
    }

    /**
     * 나이가 평균 이상인 회원 조회
     */
    @Test
    public void subQueryGoe() {

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory.selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(30, 40);
    }

    /**
     * 나이가 10보다 많은 회원 조회
     */
    @Test
    public void subQueryIn() {

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory.selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);
    }

    /**
     * 이름과 평균 나이
     */
    @Test
    public void selectSubQuery(){

        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory.select(member.username,
                        select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void basicCase(){
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void complexCase(){
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     * orderBy에서 Case문 함께 사용하기
     * - 0~30살이 아닌 회원을 가장 먼저 출력
     * - 0~20살 회원 출력
     * - 21~30살 회원 출력
     *
     * 결과
     * username = member4 age = 40 rank = 3
     * username = member1 age = 10 rank = 2
     * username = member2 age = 20 rank = 2
     * username = member3 age = 30 rank = 1
     */
    @Test
    public void caseOrderBy(){
        NumberExpression<Integer> rankPath = new CaseBuilder()
                .when(member.age.between(0, 20)).then(2)
                .when(member.age.between(21, 30)).then(1)
                .otherwise(3);

        List<Tuple> result = queryFactory.select(member.username, member.age, rankPath)
                .from(member)
                .orderBy(rankPath.desc())
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            Integer rank = tuple.get(rankPath);
            System.out.println("username = " + username + " age = " + age + " rank = " + rank);
        }
    }

    // 상수 더하기
    @Test
    public void constant(){
        List<Tuple> result = queryFactory.select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    // 문자 더하기 - stringValue()는 ENUM에서도 자주 사용함
    @Test
    public void concat(){
        // username_age
        List<String> result = queryFactory.select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }


    @Test
    public void simpleProjection(){
        List<String> result = queryFactory.select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    // Tuple은 querydsl에 종속적임으로 repository를 벗어나는 것은 좋지 않음. 꺼낼 때는 DTO로 변환해서 사용해야함.
    @Test
    public void tupleProjection(){
        List<Tuple> result = queryFactory.select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }

    @Test
    public void findDtoByJPQL(){
        List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    // 프로퍼티 접근 - Setter
    @Test
    public void findDtoBySetter(){
        // 기본 생성자를 만들고 값을 set하기 때문에 MemberDto에 기본 생성자가 없으면 에러 발생함!
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    // 필드 직접 접근
    @Test
    public void findDtoByField(){
        // fields는 getter, setter 없이 바로 가능
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    // 생성자
    @Test
    public void findDtoByConstructor(){
        // MemberDto의 타입이 각각 맞아야 한다.
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }


    // 만약, 별칭이 다른 경우 - fields
    @Test
    public void findUserDto(){
        QMember memberSub = new QMember("memberSub");

        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
//                        ExpressionUtils.as(member.username, "name"), // 위와 동일하나 위에가 깔끔함
                        ExpressionUtils.as(JPAExpressions // 서브 쿼리의 경우 ExpressionUtils 사용
                                .select(memberSub.age.max())
                                .from(memberSub), "age")
                ))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    // 만약, 별칭이 다른 경우 - 생성자
    @Test
    public void findUserDtoByConstructor(){
        // MemberDto의 타입이 각각 맞아야 한다.
        List<UserDto> result = queryFactory
                .select(Projections.constructor(UserDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    /**
     * constructor 사용과의 차이점
     * - 만약 생성자에 없는 값이 들어가는 경우..
     * -> constructor의 경우 컴파일시 오류를 잡지 못하고 런타임 오류로 확인 가능
     * -> @QueryProduction의 경우 컴파일 오류로 알 수 있음. 안전한 방법
     *
     * 단점, 실무에서 고민거리..
     * - Qtype을 생성해야한다
     * - 의존관계 문제 -> @QueryProduction를 Dto에 추가하면서 Querydsl에 의존성을 갖게 됨
     */
    @Test
    public void findDtoByQueryProjection() {
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void dynamicQuery_BooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        BooleanBuilder builder = new BooleanBuilder();
        // 앞서 null이 아닌 방어코드와 함께 초기값을 설정해 둘 수도 있다.
//        BooleanBuilder builder = new BooleanBuilder(member.username.eq(usernameCond));

        if(usernameCond != null){
            builder.and(member.username.eq(usernameCond));
        }

        if(ageCond != null){
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder) // 여기에서도 and, or 등으로 조립이 가능
                .fetch();
    }

    @Test
    public void dynamicQuery_WhereParam() {
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond))
//                .where(allEq(usernameCond, ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        // null check 포함
        return usernameCond == null ? null : member.username.eq(usernameCond);
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond == null ? null : member.age.eq(ageCond);
    }

    // 조합 가능. 단, null 체크는 주의해서 처리해야함
    private Predicate allEq(String usernameCond, Integer ageCond){
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    @Test
    @Commit
    public void bulkUpdate() {
        // 실행 전
        // member1 = 10 -> DB member1
        // member2 = 20 -> DB member2
        // member3 = 30 -> DB member3
        // member4 = 40 -> DB member4

        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute(); // 영향을 받은 로우 수가 나옴

        // 실행 후
        // member1 = 10 -> DB 비회원
        // member2 = 20 -> DB 비회원
        // member3 = 30 -> DB member3
        // member4 = 40 -> DB member4

        /** 주의!!
         * bulk 연산은 영속성 컨텍스트를 바꾸지 않고 바로 DB의 값을 변경해버림 -> 따라서, DB의 값과 영속성 컨텍스트의 값이 달라져 버린다.
         * DB에서 조회를 해도, 영속성 컨텍스트에 값이 있으면 먼저 우선권을 갖는다.
         *
         * 따라서 bulk를 사용하는 경우!!
         * 영속성 컨텍스트 초기화!!
         * 영속성 컨텍스트를 flush해서 내보내고, 초기화 해버리면 된다.
         * 이 과정이 없으면, 실행 전 DB 상태의 값이 조회된다.
         */
        em.flush();
        em.clear();

        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();

        // 영속성 컨텍스트가 우선권을 갖음
        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
    }

    @Test
    public void bulkAdd() {
        long execute = queryFactory
                .update(member)
                .set(member.age, member.age.add(1)) // 곱하기는 multiply()
                .execute();
    }

    @Test
    public void bulkDelete() {
        long execute = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }


    @Test
    public void sqlFunction() {
        List<String> result = queryFactory
                .select(Expressions.stringTemplate(
                        "function('replace', {0}, {1}, {2})",
                        member.username, "member", "M"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    // 예제보다 기능을 확인하기
    /* select
            member1.username
        from
            Member member1
        where
            member1.username = function('lower', member1.username) */
    /*
        select
            member0_.username as col_0_0_
        from
            member member0_
        where
            member0_.username=lower(member0_.username) */
    @Test
    public void sqlFunction2() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
//                .where(member.username.eq(Expressions.stringTemplate("function('lower', {0})", member.username)))
                // querydsl : 일반적인 ansi 표준에 있는 것들은 내장되어 제공함
                .where(member.username.eq(member.username.lower()))
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }



}
