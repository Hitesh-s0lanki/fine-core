package com.fincore.server.models.disclosure;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.company.CompanyRegistration;
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

/** Persistent registry filing detail assertion. */
@Entity
@Table(name = "registry_filing_detail")
@DiscriminatorValue("registry-filing-details")
@Getter
@Setter
@NoArgsConstructor
public class RegistryFilingDetail extends DomainRecord {
  @Column(name = "filing_id", nullable = false)
  private UUID filingId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "filing_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Filing filingReference;

  @Column(name = "registration_id", nullable = false)
  private UUID registrationId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "registration_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private CompanyRegistration registrationReference;

  @Column(name = "transaction_id", nullable = false)
  private String transactionId;

  @Column(name = "category", nullable = true)
  private String category;

  @Column(name = "filing_type", nullable = true)
  private String filingType;

  @Column(name = "description_code", nullable = true)
  private String descriptionCode;

  @Column(name = "description_text", nullable = true)
  private String descriptionText;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "description_values", nullable = false, columnDefinition = "jsonb")
  private Object descriptionValues;

  @Column(name = "action_date", nullable = true)
  private LocalDate actionDate;

  @Column(name = "pages", nullable = true)
  private Integer pages;
}
