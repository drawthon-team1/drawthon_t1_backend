package com.drawthon.team1.posting.config;

import com.drawthon.team1.posting.client.VolunteerApiClient;
import com.drawthon.team1.posting.dto.PostingExternalDTO;
import com.drawthon.team1.posting.repository.PostingRepository;
import com.drawthon.team1.posting.service.PostingService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class VolunteerPostingInitializerTest {
    private final VolunteerApiClient client = mock(VolunteerApiClient.class);
    private final PostingService service = mock(PostingService.class);
    private final PostingRepository repository = mock(PostingRepository.class);
    private final VolunteerPostingInitializer initializer = new VolunteerPostingInitializer(client, service, repository, 50, 0);

    private static PostingExternalDTO found(String number) {
        return PostingExternalDTO.builder().programRegistrationNumber(number).build();
    }

    @Test
    void importsNewPostingsAndSkipsAlreadySavedOnes() {
        when(client.isConfigured()).thenReturn(true);
        when(client.search(null, 1, 50)).thenReturn(List.of(found("1"), found("2"), found("3")));
        when(repository.existsByProgramRegistrationNumber("2")).thenReturn(true);

        assertThat(initializer.importPostings()).isEqualTo(2);
        verify(service).importFromVolunteerApi("1");
        verify(service, never()).importFromVolunteerApi("2");
        verify(service).importFromVolunteerApi("3");
    }

    @Test
    void oneFailureDoesNotStopTheRestAndIsRetriedOnce() {
        when(client.isConfigured()).thenReturn(true);
        when(client.search(null, 1, 50)).thenReturn(List.of(found("1"), found("2")));
        when(service.importFromVolunteerApi("1"))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to call 1365 api"));

        assertThat(initializer.importPostings()).isEqualTo(1);
        verify(service).importFromVolunteerApi("2");
        verify(service, times(2)).importFromVolunteerApi("1");
    }

    @Test
    void failureThatSucceedsOnRetryIsCounted() {
        when(client.isConfigured()).thenReturn(true);
        when(client.search(null, 1, 50)).thenReturn(List.of(found("1"), found("2")));
        when(service.importFromVolunteerApi("1"))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "429 Too Many Requests"))
                .thenReturn(null);

        assertThat(initializer.importPostings()).isEqualTo(2);
    }

    @Test
    void skipsWithoutServiceKey() {
        when(client.isConfigured()).thenReturn(false);

        assertThat(initializer.importPostings()).isZero();
        verify(client, never()).search(any(), anyInt(), anyInt());
        verifyNoInteractions(service);
    }

    @Test
    void searchFailureIsLoggedNotThrown() {
        when(client.isConfigured()).thenReturn(true);
        when(client.search(null, 1, 50)).thenThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "1365 API 호출 실패"));

        assertThat(initializer.importPostings()).isZero();
        verifyNoInteractions(service);
    }
}
