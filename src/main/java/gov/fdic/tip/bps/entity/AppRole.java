package gov.fdic.tip.bps.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents an application role stored in app_role table.
 */
@Entity
@Table(name = "app_role")
@Getter
@Setter
@NoArgsConstructor
public class AppRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "role_name", length = 100, unique = true, nullable = false)
    private String roleName;

    @Column(name = "description", length = 255)
    private String description;
}
