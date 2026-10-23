package com.fincore.server.service;

import static com.fincore.server.support.TestRecords.identified;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fincore.server.config.Resources;
import com.fincore.server.dto.common.RecordView;
import com.fincore.server.dto.company.CompanyIdentifierRequest;
import com.fincore.server.dto.company.CompanyOnboardingRequest;
import com.fincore.server.dto.company.CompanyRequest;
import com.fincore.server.dto.company.InitialIdentifier;
import com.fincore.server.interfaces.CompanyDataServiceInter;
import com.fincore.server.models.company.Company;
import com.fincore.server.repository.company.CompanyRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {
  @Mock private CompanyRepository companies;
  @Mock private CompanyDataServiceInter data;
  private CompanyService service;

  @BeforeEach
  void setUp() {
    service = new CompanyService(companies, data);
  }

  @Test
  void onboardingAssignsServerCompanyIdAndPreservesIdentifierEvidenceAndDates() {
    UUID id = UUID.randomUUID();
    UUID evidence = UUID.randomUUID();
    var request = new CompanyRequest(Set.of(evidence), "infosys", "Infosys Limited", "issuer");
    var initial =
        new InitialIdentifier(
            Set.of(evidence), "cik", "sec", "0001067491", LocalDate.of(2020, 1, 1), null);
    var companyResource = resource("companies");
    var identifierResource = resource("company-identifiers");
    Company company = identified(new Company(), id);
    RecordView view = view(id);
    when(data.resource("companies")).thenReturn(companyResource);
    when(data.resource("company-identifiers")).thenReturn(identifierResource);
    when(data.save(companyResource, request)).thenReturn(company);
    when(data.view(company)).thenReturn(view);

    assertThat(service.create(new CompanyOnboardingRequest(request, List.of(initial))))
        .isEqualTo(view);

    var captured = ArgumentCaptor.forClass(CompanyIdentifierRequest.class);
    verify(data).save(eq(identifierResource), captured.capture());
    assertThat(captured.getValue())
        .isEqualTo(
            new CompanyIdentifierRequest(
                Set.of(evidence), id, "cik", "sec", "0001067491", LocalDate.of(2020, 1, 1), null));
  }

  @Test
  void searchTrimsQueryAndReturnsCorrectPaginationMetadata() {
    UUID id = UUID.randomUUID();
    Company company = identified(new Company(), id);
    var pageable = PageRequest.of(1, 2, Sort.by("displayName", "id"));
    when(companies.search("INFY", pageable))
        .thenReturn(new PageImpl<>(List.of(company), pageable, 5));
    when(data.view(company)).thenReturn(view(id));

    var result = service.search("  INFY  ", 1, 2);

    assertThat(result.items()).extracting(RecordView::id).containsExactly(id);
    assertThat(result.totalItems()).isEqualTo(5);
    assertThat(result.page()).isEqualTo(1);
    assertThat(result.size()).isEqualTo(2);
    assertThat(result.hasNext()).isTrue();
  }

  @ParameterizedTest
  @CsvSource({"-1,25", "0,0", "0,101"})
  void invalidPaginationNeverQueriesTheRepository(int page, int size) {
    assertThatThrownBy(() -> service.search("Infosys", page, size))
        .isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(companies, data);
  }

  @Test
  void oversizedSearchQueryIsRejectedBeforeDatabaseAccess() {
    assertThatThrownBy(() -> service.search("a".repeat(101), 0, 25))
        .isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(companies, data);
  }

  @Test
  void companyReadsUseTheCompanyResource() {
    UUID id = UUID.randomUUID();
    when(data.get("companies", id)).thenReturn(view(id));
    assertThat(service.get(id)).isEqualTo(view(id));
    verifyNoInteractions(companies);
  }

  private Resources.Resource resource(String route) {
    return Resources.ALL.stream().filter(r -> r.route().equals(route)).findFirst().orElseThrow();
  }

  private RecordView view(UUID id) {
    return new RecordView(
        id, "companies", 1, null, Set.of(), Map.of("displayName", "Infosys Limited"));
  }
}
