package com.uno.getinline.service;

import com.uno.getinline.constant.ErrorCode;
import com.uno.getinline.constant.EventStatus;
import com.uno.getinline.dto.EventDTO;
import com.uno.getinline.exception.GeneralException;
import com.uno.getinline.repository.EventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @InjectMocks private EventService sut;
    @Mock private EventRepository eventRepository;

    @DisplayName("검색 조건 없이 이벤트를 검색하면, 전체 결과를 출력하여 보여준다.")
    @Test
    void givenNothing_whenSearchingEvents_thenReturnsEntireEventList() {
        // Given
        given(eventRepository.findEvents(null, null, null, null, null))
                .willReturn(List.of(
                        createEventDTO(1L, "오전 운동", true),
                        createEventDTO(1L, "오후 운동", false)
                ));

        // When
        List<EventDTO> list = sut.getEvents(null, null, null, null, null);

        // Then
        assertThat(list).hasSize(2);
        then(eventRepository).should().findEvents(null, null, null, null, null);
    }

    @DisplayName("검색 조건과 함께 이벤트를 검색하면, 검색 결과를 출력하여 보여준다.")
    @Test
    void givenSearchParameters_whenSearchingEvents_thenReturnsEventList() {
        // Given
        Long placeId = 1L;
        String eventName = "오전 운동";
        EventStatus eventStatus = EventStatus.OPENED;
        LocalDateTime eventStartDatetime = LocalDateTime.of(2021, 1, 1, 0, 0, 0);
        LocalDateTime eventEndDatetime = LocalDateTime.of(2021, 1, 2, 0, 0, 0);

        given(eventRepository.findEvents(placeId, eventName, eventStatus, eventStartDatetime, eventEndDatetime))
                .willReturn(List.of(
                        createEventDTO(1L, "오전 운동", eventStatus, eventStartDatetime, eventEndDatetime)
                ));

        // When
        List<EventDTO> list = sut.getEvents(placeId, eventName, eventStatus, eventStartDatetime, eventEndDatetime);

        // Then
        assertThat(list)
                .hasSize(1)
                .allSatisfy(event -> {
                    assertThat(event)
                            .hasFieldOrPropertyWithValue("placeId", placeId)
                            .hasFieldOrPropertyWithValue("eventName", eventName)
                            .hasFieldOrPropertyWithValue("eventStatus", eventStatus);
                    assertThat(event.eventStartDatetime()).isAfterOrEqualTo(eventStartDatetime);
                    assertThat(event.eventEndDatetime()).isBeforeOrEqualTo(eventEndDatetime);
                });
        then(eventRepository).should().findEvents(placeId, eventName, eventStatus, eventStartDatetime, eventEndDatetime);
    }

    @DisplayName("이벤트를 검색하는데 에러가 발생한 경우, 줄서기 프로젝트 기본 에러로 전환하여 예외 던진다.")
    @Test
    void givenDataRelatedException_whenSearchingEvents_thenThrowsGeneralException() {
        // Given
        RuntimeException e = new RuntimeException("This is test.");
        given(eventRepository.findEvents(any(), any(), any(), any(), any())).willThrow(e);

        // When
        Throwable thrown = catchThrowable(() -> sut.getEvents(null, null, null, null, null));

        // Then
        assertThat(thrown)
                .isInstanceOf(GeneralException.class)
                .hasMessageContaining(ErrorCode.DATA_ACCESS_ERROR.getMessage());
        then(eventRepository).should().findEvents(any(), any(), any(), any(), any());
    }

    @DisplayName("이벤트 ID로 존재하는 이벤트를 조회하면, 해당 이벤트 정보를 출력하여 보여준다.")
    @Test
    void givenEventId_whenSearchingExistingEvent_thenReturnsEvent() {
        // Given
        long eventId = 1L;
        EventDTO eventDTO = createEventDTO(1L, "오전 운동", true);
        given(eventRepository.findEvent(eventId)).willReturn(Optional.of(eventDTO));

        // When
        Optional<EventDTO> result = sut.getEvent(eventId);

        // Then
        assertThat(result).hasValue(eventDTO);
        then(eventRepository).should().findEvent(eventId);
    }

    @DisplayName("이벤트 ID로 이벤트를 조회하면, 빈 정보를 출력하여 보여준다.")
    @Test
    void givenEventId_whenSearchingNonexistentEvent_thenReturnsEmptyOptional() {
        // Given
        long eventId = 2L;
        given(eventRepository.findEvent(eventId)).willReturn(Optional.empty());

        // When
        Optional<EventDTO> result = sut.getEvent(eventId);

        // Then
        assertThat(result).isEmpty();
        then(eventRepository).should().findEvent(eventId);
    }

    @DisplayName("이벤트 ID로 이벤트를 조회하는데 데이터 관련 에러가 발생한 경우, 줄서기 프로젝트 기본 에러로 전환하여 예외 던진다.")
    @Test
    void givenDataRelatedException_whenSearchingEvent_thenThrowsGeneralException() {
        // Given
        RuntimeException e = new RuntimeException("This is test.");
        given(eventRepository.findEvent(any())).willThrow(e);

        // When
        Throwable thrown = catchThrowable(() -> sut.getEvent(null));

        // Then
        assertThat(thrown)
                .isInstanceOf(GeneralException.class)
                .hasMessageContaining(ErrorCode.DATA_ACCESS_ERROR.getMessage());
        then(eventRepository).should().findEvent(any());
    }

    @DisplayName("이벤트 정보를 주면, 이벤트를 생성하고 결과를 true 로 보여준다.")
    @Test
    void givenEvent_whenCreating_thenCreatesEventAndReturnsTrue() {
        // Given
        EventDTO dto = createEventDTO(1L, "오후 운동", false);
        given(eventRepository.insertEvent(dto)).willReturn(true);

        // When
        boolean result = sut.createEvent(dto);

        // Then
        assertThat(result).isTrue();
        then(eventRepository).should().insertEvent(dto);
    }

    @DisplayName("이벤트 정보를 주지 않으면, 생성 중단하고 결과를 false 로 보여준다.")
    @Test
    void givenNothing_whenCreating_thenAbortCreatingAndReturnsFalse() {
        // Given
        given(eventRepository.insertEvent(null)).willReturn(false);

        // When
        boolean result = sut.createEvent(null);

        // Then
        assertThat(result).isFalse();
        then(eventRepository).should().insertEvent(null);
    }

    @DisplayName("이벤트 생성 중 데이터 예외가 발생하면, 줄서기 프로젝트 기본 에러로 전환하여 예외 던진다")
    @Test
    void givenDataRelatedException_whenCreating_thenThrowsGeneralException() {
        // Given
        RuntimeException e = new RuntimeException("This is test.");
        given(eventRepository.insertEvent(any())).willThrow(e);

        // When
        Throwable thrown = catchThrowable(() -> sut.createEvent(null));

        // Then
        assertThat(thrown)
                .isInstanceOf(GeneralException.class)
                .hasMessageContaining(ErrorCode.DATA_ACCESS_ERROR.getMessage());
        then(eventRepository).should().insertEvent(any());
    }

    @DisplayName("이벤트 ID와 정보를 주면, 이벤트 정보를 변경하고 결과를 true 로 보여준다.")
    @Test
    void givenEventIdAndItsInfo_whenModifying_thenModifiesEventAndReturnsTrue() {
        // Given
        long eventId = 1L;
        EventDTO dto = createEventDTO(1L, "오후 운동", false);
        given(eventRepository.updateEvent(eventId, dto)).willReturn(true);

        // When
        boolean result = sut.modifyEvent(eventId, dto);

        // Then
        assertThat(result).isTrue();
        then(eventRepository).should().updateEvent(eventId, dto);
    }

    @DisplayName("이벤트 ID를 주지 않으면, 이벤트 정보 변경 중단하고 결과를 false 로 보여준다.")
    @Test
    void givenNoEventId_whenModifying_thenAbortModifyingAndReturnsFalse() {
        // Given
        EventDTO dto = createEventDTO(1L, "오후 운동", false);
        given(eventRepository.updateEvent(null, dto)).willReturn(false);

        // When
        boolean result = sut.modifyEvent(null, dto);

        // Then
        assertThat(result).isFalse();
        then(eventRepository).should().updateEvent(null, dto);
    }

    @DisplayName("이벤트 ID만 주고 변경할 정보를 주지 않으면, 이벤트 정보 변경 중단하고 결과를 false 로 보여준다.")
    @Test
    void givenEventIdOnly_whenModifying_thenAbortModifyingAndReturnsFalse() {
        // Given
        long eventId = 1L;
        given(eventRepository.updateEvent(eventId, null)).willReturn(false);

        // When
        boolean result = sut.modifyEvent(eventId, null);

        // Then
        assertThat(result).isFalse();
        then(eventRepository).should().updateEvent(eventId, null);
    }

    @DisplayName("이벤트 변경 중 데이터 오류가 발생하면, 줄서기 프로젝트 기본 에러로 전환하여 예외 던진다.")
    @Test
    void givenDataRelatedException_whenModifying_thenThrowsGeneralException() {
        // Given
        RuntimeException e = new RuntimeException("This is test.");
        given(eventRepository.updateEvent(any(), any())).willThrow(e);

        // When
        Throwable thrown = catchThrowable(() -> sut.modifyEvent(null, null));

        // Then
        assertThat(thrown)
                .isInstanceOf(GeneralException.class)
                .hasMessageContaining(ErrorCode.DATA_ACCESS_ERROR.getMessage());
        then(eventRepository).should().updateEvent(any(), any());
    }

    @DisplayName("이벤트 ID를 주면, 이벤트 정보를 삭제하고 결과를 true 로 보여준다.")
    @Test
    void givenEventId_whenDeleting_thenDeletesEventAndReturnsTrue() {
        // Given
        long eventId = 1L;
        given(eventRepository.deleteEvent(eventId)).willReturn(true);

        // When
        boolean result = sut.removeEvent(eventId);

        // Then
        assertThat(result).isTrue();
        then(eventRepository).should().deleteEvent(eventId);
    }

    @DisplayName("이벤트 ID를 주지 않으면, 삭제 중단하고 결과를 false 로 보여준다.")
    @Test
    void givenNothing_whenDeleting_thenAbortsDeletingAndReturnsFalse() {
        // Given
        given(eventRepository.deleteEvent(null)).willReturn(false);

        // When
        boolean result = sut.removeEvent(null);

        // Then
        assertThat(result).isFalse();
        then(eventRepository).should().deleteEvent(null);
    }

    @DisplayName("이벤트 삭제 중 데이터 오류가 발생하면, 줄서기 프로젝트 기본 에러로 전환하여 예외 던진다.")
    @Test
    void givenDataRelatedException_whenDeleting_thenThrowsGeneralException() {
        // Given
        RuntimeException e = new RuntimeException("This is test.");
        given(eventRepository.deleteEvent(any())).willThrow(e);

        // When
        Throwable thrown = catchThrowable(() -> sut.removeEvent(null));

        // Then
        assertThat(thrown)
                .isInstanceOf(GeneralException.class)
                .hasMessageContaining(ErrorCode.DATA_ACCESS_ERROR.getMessage());
        then(eventRepository).should().deleteEvent(any());
    }


    private EventDTO createEventDTO(long placeId, String eventName, boolean isMorning) {
        String hourStart = isMorning ? "09" : "13";
        String hourEnd = isMorning ? "12" : "16";

        return createEventDTO(
                placeId,
                eventName,
                EventStatus.OPENED,
                LocalDateTime.parse("2021-01-01T%s:00:00".formatted(hourStart)),
                LocalDateTime.parse("2021-01-01T%s:00:00".formatted(hourEnd))
        );
    }

    private EventDTO createEventDTO(
            long placeId,
            String eventName,
            EventStatus eventStatus,
            LocalDateTime eventStartDateTime,
            LocalDateTime eventEndDateTime
    ) {
        return EventDTO.of(
                1L,
                placeId,
                eventName,
                eventStatus,
                eventStartDateTime,
                eventEndDateTime,
                0,
                24,
                "마스크 꼭 착용하세요",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

}