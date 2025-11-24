package com.example.duokid.repo;

import com.example.duokid.model.ShopTransaction;
import com.example.duokid.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShopTransactionRepository extends JpaRepository<ShopTransaction, Long> {
    List<ShopTransaction> findTop20ByUserOrderByTimeDesc(User user);
}
