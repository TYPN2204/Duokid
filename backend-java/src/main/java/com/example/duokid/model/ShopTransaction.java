package com.example.duokid.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "shop_transactions")
public class ShopTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    private LocalDateTime time;

    private int heartsChanged;
    private int xpChanged;

    private String type;
    private String note;

    public ShopTransaction() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDateTime getTime() { return time; }
    public void setTime(LocalDateTime time) { this.time = time; }

    public int getHeartsChanged() { return heartsChanged; }
    public void setHeartsChanged(int heartsChanged) { this.heartsChanged = heartsChanged; }

    public int getXpChanged() { return xpChanged; }
    public void setXpChanged(int xpChanged) { this.xpChanged = xpChanged; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
