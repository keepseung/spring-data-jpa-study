package study.datajpa.repository;

import jdk.jfr.Enabled;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import study.datajpa.dto.MemberDto;
import study.datajpa.entity.Member;

import javax.persistence.QueryHint;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {

    // 스프링 데이터 JPA는 메소드 이름을 분석해서 JPQL을 생성하고 실행
    List<Member> findByUsernameAndAgeGreaterThan(String username, int age);

    // 스프링 데이터 JPA로 NamedQuery 사용
    // @Query를 생략하고 메서드 이름만으로 Named 쿼리를 호출할 수 있다.
    // @Query(name = "Member.findByUsername")
    List<Member> findByUsername(@Param("username") String username);

    // 메서드에 JPQL 쿼리 작성
    // JPA Named 쿼리처럼 애플리케이션 실행 시점에 문법 오류를 발견할 수 있음
    @Query("select m from Member m where m.username= :username and m.age = :age")
    List<Member> findUser(@Param("username") String username, @Param("age") int age);

    // 단순히 값 하나를 조회
    @Query("select m.username from Member m")
    List<String> findUsernameList();

    // DTO로 직접 조회
    // DTO로 직접 조회 하려면 JPA의 new 명령어를 사용해야 한다.
    // 그리고 다음과 같이 생성자가 맞는 DTO가 필요하다. (JPA와 사용방식이 동일하다.)
    @Query("select new study.datajpa.dto.MemberDto(m.id, m.username, t.name) " +
            "from Member m join m.team t")
    List<MemberDto> findMemberDto();

    // 파라미터 바인딩
    // 파라미터 바인딩은 위치 기반, 이름 기반이 있다.
    // 코드 가독성과 유지보수를 위해 이름 기반 파라미터 바인딩을 사용하자 (위치기반은 파라미터의 순서가 틀릴 경우 힘들다..)
    @Query("select m from Member m where m.username = :name")
    Member findMembers(@Param("name") String username);

    // 컬렉션 파라미터 바인딩
    // Collection 타입으로 in절 지원
    @Query("select m from Member m where m.username in :names")
    List<Member> findByNames(@Param("names") List<String> names);


    /**
     * 스프링 데이터 JPA는 유연한 반환 타입 지원
     * 조회 결과가 많거나 없으면?
        * 컬렉션
            * 결과 없음: 빈 컬렉션 반환 단건 조회
            * 결과 없음: null 반환
        * 단건 조회
            * 결과가 2건 이상: javax.persistence.NonUniqueResultException 예외 발생
      **/
    // List<Member> findByUsername(String name); //컬렉션
    // Member findByUsername(String name); //단건
    // Optional<Member> findByUsername(String name); //단건 Optional

    /**
     * 스프링 데이터 JPA 페이징과 정렬
     * - 페이징과 정렬 파라미터
     *  Sort : 정렬 기능
     *  Pageable : 페이징 기능 (내부에 Sort 포함)
     * - 특별한 반환 타입
     *  Page : 추가 count 쿼리 결과를 포함하는 페이징
     *  Slice : 추가 count 쿼리 없이 다음 페이지만 확인 가능
     */
//    Page<Member> findByUsername(String name, Pageable pageable); //count 쿼리 사용
    // Slice<Member> findByUsername(String name, Pageable pageable); //count 쿼리 사용 안함
    // List<Member> findByUsername(String name, Pageable pageable); //count 쿼리 사용 안함
    // List<Member> findByUsername(String name, Sort sort);

    Page<Member> findByAge(int age, Pageable pageable);

    // count 쿼리를 다음과 같이 분리할 수 있음
    @Query(value = "select m from Member m",
            countQuery = "select count(m.username) from Member m")
    Page<Member> findMemberAllCountBy(Pageable pageable);

    // 스프링 데이터 JPA를 사용한 벌크성 수정 쿼리
    // 벌크성 수정, 삭제 쿼리는 @Modifying 어노테이션을 사용
    // 만약 다시 조회해야 하면 clearAutomatically = true를 통해 꼭 영속성 컨텍스트를 초기화 하자.
    // @Modifying(clearAutomatically = true)
    @Modifying
    @Query("update Member m set m.age = m.age + 1 where m.age >= :age")
    int bulkAgePlus(@Param("age") int age);

    // Fetch 조인
    @Query("select m from Member m left join fetch m.team")
    List<Member> findMemberFetchJoin();

    // EntityGraph
    // Fetch 조인과 같이 연관된 테이블의 값도 가져옴
//    @Override
//    @EntityGraph(attributePaths ={"team"})
//    List<Member> findAll();

    // EntityGraph
    // Fetch 조인과 같이 연관된 테이블의 값도 가져옴
    @EntityGraph(attributePaths ={"team"})
    @Query("select m from Member m")
    List<Member> findMemberEntityGraph();

    @EntityGraph(attributePaths ={"team"})
    List<Member> findEntityGraphByUsername(@Param("username") String username);


    // 영속성 컨텍스트에 값을 가지고 있지 않아서 Update 쿼리 실행 안됨
    // 성능이 그렇게 최적화가 안됨
    @QueryHints(value = @QueryHint(name = "org.hibernate.readOnly", value =
            "true"))
    Member findReadOnlyByUsername(String username);

    // native 쿼리 사용
    @Query(value = "select * from member where username = ?", nativeQuery =
            true)
    Member findByNativeQuery(String username);
}
