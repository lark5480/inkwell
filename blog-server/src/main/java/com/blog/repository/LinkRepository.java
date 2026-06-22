package com.blog.repository;

import com.blog.entity.Link;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LinkRepository extends JpaRepository<Link, Long> {

    List<Link> findByStatus(Integer status);

    List<Link> findByIsDeletedOrderBySort(Boolean isDeleted);
}
