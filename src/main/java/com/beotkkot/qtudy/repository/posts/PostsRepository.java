package com.beotkkot.qtudy.repository.posts;

import com.beotkkot.qtudy.domain.posts.Posts;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostsRepository extends JpaRepository<Posts, Long> {
    Page<Posts> findAllByKakaoId(Long kakaoId, PageRequest pageRequest);

    @Query("SELECT p FROM Posts p WHERE p.title LIKE %:searchWord% OR p.content LIKE %:searchWord% OR p.tag LIKE %:searchWord%")
    Page<Posts> findBySearchWord(String searchWord, PageRequest pageRequest);

    @Query("SELECT p FROM Posts p WHERE p.categoryId IN :categoryIds")
    Page<Posts> findByCategoryIds(@Param("categoryIds")List<Long> categoryIds, PageRequest pageRequest);

    Posts findByPostId(Long postId);

    @Query("SELECT p FROM Posts p JOIN Scrap s ON p.postId = s.postId WHERE s.postId IN :postIds ORDER BY s.scrapAt DESC")
    List<Posts> findAllByPostId(List<Long> postIds);

    @Query("SELECT p FROM Posts p JOIN Scrap s ON p.postId = s.postId WHERE s.postId IN :postIds ORDER BY s.scrapAt DESC")
    Page<Posts> findByPostIds(@Param("postIds") List<Long> postIds, PageRequest pageRequest);
}
