package com.example.duokid.controller;

import com.example.duokid.model.ShopTransaction;
import com.example.duokid.model.User;
import com.example.duokid.repo.ShopTransactionRepository;
import com.example.duokid.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.List;

@Controller
public class ShopController {

    private final UserService userService;
    private final ShopTransactionRepository shopTxRepo;
    private static final int XP_PER_HEART = 10;

    public ShopController(UserService userService,
                          ShopTransactionRepository shopTxRepo) {
        this.userService = userService;
        this.shopTxRepo = shopTxRepo;
    }

    @GetMapping("/shop")
    public String shop(@RequestParam(required = false) String reason,
                       HttpSession session,
                       Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        List<ShopTransaction> history = shopTxRepo.findTop20ByUserOrderByTimeDesc(user);

        model.addAttribute("user", user);
        model.addAttribute("reason", reason);
        model.addAttribute("xpPerHeart", XP_PER_HEART);
        model.addAttribute("history", history);
        return "shop";
    }

    @PostMapping("/shop/buy-hearts")
    public String buyHearts(@RequestParam int amount,
                            @RequestParam(defaultValue = "XP") String paymentType,
                            HttpSession session,
                            Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        List<ShopTransaction> history = shopTxRepo.findTop20ByUserOrderByTimeDesc(user);
        model.addAttribute("history", history);
        model.addAttribute("user", user);
        model.addAttribute("xpPerHeart", XP_PER_HEART);

        if (amount <= 0) {
            model.addAttribute("error", "Số tim phải lớn hơn 0.");
            return "shop";
        }

        ShopTransaction tx = new ShopTransaction();
        tx.setUser(user);
        tx.setTime(LocalDateTime.now());
        tx.setHeartsChanged(amount);

        if ("GEMS".equals(paymentType)) {
            // Buy with gems (200 gems per heart)
            int gemsPerHeart = 200;
            int cost = amount * gemsPerHeart;
            if (user.getGems() < cost) {
                model.addAttribute("error", "Gems không đủ. Cần " + cost + " Gems để mua " + amount + " tim.");
                return "shop";
            }
            user.setGems(user.getGems() - cost);
            tx.setXpChanged(0);
            tx.setType("BUY_GEMS");
            tx.setNote("Mua tim bằng Gems");
        } else {
            // Buy with XP (default)
            int cost = amount * XP_PER_HEART;
            if (user.getXp() < cost) {
                model.addAttribute("error", "XP không đủ. Cần " + cost + " XP để mua " + amount + " tim.");
                return "shop";
            }
            user.setXp(user.getXp() - cost);
            tx.setXpChanged(-cost);
            tx.setType("BUY");
            tx.setNote("Mua tim bằng XP");
        }

        user.setHearts(user.getHearts() + amount);
        userService.save(user);
        session.setAttribute("user", user);
        shopTxRepo.save(tx);

        history = shopTxRepo.findTop20ByUserOrderByTimeDesc(user);
        model.addAttribute("history", history);
        model.addAttribute("success", "Mua thành công " + amount + " tim! ❤️");
        return "shop";
    }
}
