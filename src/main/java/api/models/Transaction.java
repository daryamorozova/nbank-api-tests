package api.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.security.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Transaction {
    private long id;
    private double amount;
    private String type;
    private String timestamp;
    private long relatedAccountId;
}
