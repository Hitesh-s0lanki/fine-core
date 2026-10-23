package com.fincore.server.models.company;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fincore.server.models.common.DomainRecord;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Persistent company profile observation assertion. */
@Entity
@Table(name = "company_profile_observation")
@DiscriminatorValue("profile-observations")
@Getter
@Setter
@NoArgsConstructor
public class CompanyProfileObservation extends DomainRecord {
  @Column(name = "company_id", nullable = false)
  private UUID companyId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Company companyReference;

  @Column(name = "field", nullable = false)
  private String field;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "profile_value", nullable = false, columnDefinition = "jsonb")
  private Object profileValue;

  @Column(name = "effective_from", nullable = true)
  private LocalDate effectiveFrom;

  @Column(name = "effective_to", nullable = true)
  private LocalDate effectiveTo;
}
