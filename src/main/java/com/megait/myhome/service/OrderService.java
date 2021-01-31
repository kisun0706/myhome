package com.megait.myhome.service;

import com.megait.myhome.domain.*;
import com.megait.myhome.repository.ItemRepository;
import com.megait.myhome.repository.MemberRepository;
import com.megait.myhome.repository.OrderItemRepository;
import com.megait.myhome.repository.OrderRepository;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class OrderService {

    private final MemberRepository memberRepository;
    private final ItemRepository itemRepository;
    private final OrderRepository orderRepository;
    private final ItemService itemService;
    private final OrderItemRepository orderItemRepository;

    @PostConstruct
    @Transactional
    protected void setLikeTestUser(){
        Member member=memberRepository.findByEmail("a@a.a");
        itemService.addLikes(member, 31L);
        itemService.addLikes(member, 32L);
        itemService.addLikes(member, 33L);
        itemService.addLikes(member, 34L);
    }

    @Transactional
    public void addCart(Member member, List<Long> idList) {
        // 1. itemId 에 해당하는 Item 엔티티들을 조회한다.
        member=memberRepository.getOne(member.getId()); // 리턴자료형: Member (있는게 확실하면)
        // memberRepository.findById(member.getId());  // 리턴자료형: Optional<Member> (있는게 불확실하면)

        // 3. Order 테이블에서 member 가 현재 유저이고, status 가 CART 인 엔티티를 조회한다.
        //    없으면 - Order 객체를 하나 만든다.
        //    있으면 - Order 의 orderItems 를 활용한다.
        Optional<Order> orderOptional = orderRepository.findByStatusAndMember(Status.CART, member);
        Order order=null;
        if (orderOptional.isEmpty()) {
            order= Order.builder()
                    .status(Status.CART)
                    .member(member)
                    .build();
            orderRepository.save(order);
        }
        else {
            order=orderOptional.get();
        }

        // 2. Item 엔티티들을 가지고 OrderItem 엔티티(객체)를 생성 및 저장한다.
        List<Item> itemList=itemRepository.findAllById(idList);
        Order finalOrder = order;

        List<OrderItem> orderItemList=itemList.stream().map(item -> {
            return OrderItem.builder()
                            .item(item)
                            .order(finalOrder)
                            .count(1)   // 수량 (임시로 1개)
                            .orderPrice(item.getPrice())    // 구매가(여기서 할인이 들어갈 수 있음)
                            .build();
            }
        ).collect(Collectors.toList());


        // 4. (2)번에서 생성된 엔티티들을 묶어서 Order 의 orderItems(List) 에 add() 한다.
        // DB 에서 order 를 담갔다가 뺌.(orderItems 리스트 로딩)
        order=orderRepository.getOne(finalOrder.getId());

        if (order.getOrderItems() == null){ // 리스트가 없으면
            order.setOrderItems(new ArrayList<>());
        }
        order.getOrderItems().addAll(orderItemList);
    }

    public List<OrderItem> getCart(Member member){
        Optional<Order> orderOptional = orderRepository.findByStatusAndMember(Status.CART, member);
        if (orderOptional.isEmpty() || orderOptional.get().getOrderItems().isEmpty()) {
            throw new IllegalStateException("empty.cart");
        }
        return orderOptional.get().getOrderItems();
    }

    public int getTotalPrice(List<OrderItem> list){
        return list.stream().mapToInt(orderItem -> orderItem.getItem().getPrice()).sum();
    }

    public void deleteCart(List<Long> idList) {

        List<OrderItem> orderItemList=orderItemRepository.findAllById(idList);
        orderItemRepository.deleteAll(orderItemList);
    }
}











