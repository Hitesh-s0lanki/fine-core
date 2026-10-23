package com.fincore.server.models.credit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.company.Company;
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

/** Persistent credit rating observation assertion. */
@Entity
@Table(name = "credit_rating_observation")
@DiscriminatorValue("credit-rating-observations")
@Getter
@Setter
@NoArgsConstructor
public class CreditRatingObservation extends DomainRecord {
  @Column(name = "company_id", nullable = false)
  private UUID companyId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Company companyReference;

  @Column(name = "debt_instrument_id", nullable = true)
  private UUID debtInstrumentId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "debt_instrument_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private DebtInstrument debtInstrumentReference;

  @Column(name = "agency", nullable = false)
  private String agency;

  @Column(name = "rating_scale", nullable = false)
  private String ratingScale;

  @Column(name = "rating", nullable = false)
  private String rating;

  @Column(name = "outlook", nullable = true)
  private String outlook;

  @Column(name = "effective_date", nullable = false)
  private LocalDate effectiveDate;
}
