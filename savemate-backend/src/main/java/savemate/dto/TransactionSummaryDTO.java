package savemate.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionSummaryDTO {
    
    private Double totalExpenses;
    private Double totalSavings;
    private Integer transactionCount;
    private List<TransactionDTO> transactions;
    
    public TransactionSummaryDTO(Double totalExpenses, Double totalSavings, List<TransactionDTO> transactions) {
        this.totalExpenses = totalExpenses != null ? totalExpenses : 0.0;
        this.totalSavings = totalSavings != null ? totalSavings : 0.0;
        this.transactionCount = transactions != null ? transactions.size() : 0;
        this.transactions = transactions;
    }
}