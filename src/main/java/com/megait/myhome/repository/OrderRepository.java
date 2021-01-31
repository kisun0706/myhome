package com.megait.myhome.repository;

import com.megait.myhome.domain.Member;
import com.megait.myhome.domain.Order;
import com.megait.myhome.domain.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // Order 중에서 member, status (Cart) 조회

    // select * from orders where status=? and member=?
    Optional<Order> findByStatusAndMember(Status status, Member member);
}
