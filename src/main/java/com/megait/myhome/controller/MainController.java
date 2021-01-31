package com.megait.myhome.controller;

import com.google.gson.JsonObject;
import com.megait.myhome.domain.*;
import com.megait.myhome.repository.ItemRepository;
import com.megait.myhome.repository.MemberRepository;
import com.megait.myhome.service.CurrentUser;
import com.megait.myhome.service.ItemService;
import com.megait.myhome.service.MemberService;
import com.megait.myhome.service.OrderService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;


import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Controller
@AllArgsConstructor
public class MainController {

    private final ItemService itemService;
    private final MemberService memberService;
    private final OrderService orderService;

    private final Logger logger = LoggerFactory.getLogger(getClass());


    @GetMapping("/")
    public String home(@CurrentUser Member member, Model model){

        List<Album> albumList = itemService.getAlbumList();
        List<Book> bookList = itemService.getBookList();

        model.addAttribute("albumList", albumList);
        model.addAttribute("bookList", bookList);

        logger.info(albumList.toString());
        logger.info(bookList.toString());

        if(member != null){
            model.addAttribute(member);
        }
        return "view/index";
    }

    @GetMapping("/login")
    public String loginForm() {
        return "view/user/login";
    }

    @GetMapping("/store/detail")
    public String detail(Long id, Model model){

        Item item = itemService.getItem(id);
        model.addAttribute("item",item);

        return "view/store/detail";
    }

    @GetMapping("/store/like")
    @ResponseBody   // 리턴값 (String)은 view 이름이 아니라 responseBody 부분이다!
    public String addLike(@CurrentUser Member member, Long id){

        JsonObject object=new JsonObject();

        // id: 찜 당할 상품의 id
        // member: 현재 로그인한 유저

        // Item: liked 1 증가
        // Member: likes 리스트에 해당 item 추가

        try {
            itemService.addLikes(member, id);
            object.addProperty("result", true);
            object.addProperty("message", "찜 목록에 등록되었습니다.");
        }
        catch (IllegalStateException e){
            object.addProperty("result", false);
            object.addProperty("message", e.getMessage());
        }
        logger.info("찜 결과: "+object.toString());
        return object.toString();
    }

    @GetMapping("/cart/cart")   // detail.html 에서 상품을 장바구니에 담는 기능.(ajax, Modal 이용해서)
    @ResponseBody
    public String addCart(@CurrentUser Member member, Long id){

        JsonObject object=new JsonObject();

        try {
            orderService.addCart(member, List.of(id));
            object.addProperty("result2", true);
            object.addProperty("message", "장바구니에 등록되었습니다.");
        }
        catch (IllegalStateException e){
            object.addProperty("result2", false);
            object.addProperty("message", "실패");
        }
        return object.toString();
    }

    @GetMapping("/store/like-list")
    public String likeListForm(@CurrentUser Member member, Model model){

        List<Item> likes = memberService.getLikeList(member);
        model.addAttribute("likes", likes);

        return "view/store/like-list";
    }

    @PostMapping("/cart/list")  // 장바구니에 상품을 추가한 뒤, 장바구니 DB 조회 후 뷰로 이동.
    public String addCart(@CurrentUser Member member, @RequestParam("item_id") String[] itemId, Model model){
        // Order > Status.CART
        // OrderItem
        // Member > orders (List<Order>)

        // String[] ==> List<Long>
        List<Long> idList=List.of(Arrays.stream(itemId).map(Long::parseLong).toArray(Long[]::new));
        orderService.addCart(member, idList);

        // 5. 찜목록에 있었던 상품 엔티티들을 삭제한다.
        itemService.deleteLikes(member, idList);

        // 장바구니 보기 페이지
        return cartList(member, model);
    }

    @GetMapping("/cart/list")   // 장바구니 DB 조회 후 뷰로 이동.
    public String cartList(@CurrentUser Member member, Model model) {

        try {
            // 현재 유저의 장바구니 받기
            List<OrderItem> cartList=orderService.getCart(member);
            model.addAttribute("cartList", cartList);
            model.addAttribute("totalPrice", orderService.getTotalPrice(cartList));
        }
        catch (IllegalStateException e){
            model.addAttribute("error_message", e.getMessage()); // empty.cart
        }
        return "view/cart/list";
    }

    @PostMapping("/cart/delete")
    public String cartDelete(@CurrentUser Member member,
                             @RequestParam(value = "item_id", required = false) String[] itemIds,
                             Model model){

        if(itemIds != null && itemIds.length!=0){
            List<Long> idList=List.of(Arrays.stream(itemIds).map(Long::parseLong).toArray(Long[]::new));
            orderService.deleteCart(idList);
        }
        return cartList(member, model);
    }

}












