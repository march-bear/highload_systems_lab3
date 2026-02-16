package org.itmo.secs.domain.model.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
@Entity
@NoArgsConstructor
@Table(name = "subscriptions")
public class Subscription {
    @Id
    @Column(name = "user_id")
    Long userId;
}
