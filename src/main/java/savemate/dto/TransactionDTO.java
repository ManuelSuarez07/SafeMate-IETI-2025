package safemate.dto;

import java.time.LocalDateTime;

public class TransactionDTO {
    public Long id;
    public Long userId;
    public String merchant;
    public String category;
    public Long amountCents;
    public LocalDateTime occurredAt;
}