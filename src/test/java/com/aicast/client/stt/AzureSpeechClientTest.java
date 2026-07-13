package com.aicast.client.stt;

import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.util.EventHandler;
import com.microsoft.cognitiveservices.speech.util.EventHandlerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AzureSpeechClientTest {

    private AzureSpeechClient azureSpeechClient;

    @BeforeEach
    void setUp() {
        azureSpeechClient = new AzureSpeechClient("mock-key", "mock-region");
    }

    @Test
    @DisplayName("STT 음성 파일 연속 인식 성공 검증")
    void speechToText_Success() throws Exception {
        // Given
        MultipartFile audioFile = new MockMultipartFile("audio", "test.wav", "audio/wav", new byte[]{1, 2, 3});

        try (MockedStatic<SpeechConfig> mockedSpeechConfig = mockStatic(SpeechConfig.class);
             MockedStatic<AudioConfig> mockedAudioConfig = mockStatic(AudioConfig.class)) {

            SpeechConfig mockSpeechConfig = mock(SpeechConfig.class);
            AudioConfig mockAudioConfig = mock(AudioConfig.class);

            mockedSpeechConfig.when(() -> SpeechConfig.fromSubscription("mock-key", "mock-region")).thenReturn(mockSpeechConfig);
            mockedAudioConfig.when(() -> AudioConfig.fromWavFileInput(anyString())).thenReturn(mockAudioConfig);

            EventHandlerImpl<SpeechRecognitionEventArgs> recognizedSignal = mock(EventHandlerImpl.class);
            EventHandlerImpl<SpeechRecognitionCanceledEventArgs> canceledSignal = mock(EventHandlerImpl.class);
            EventHandlerImpl<SessionEventArgs> sessionStoppedSignal = mock(EventHandlerImpl.class);

            ArgumentCaptor<EventHandler<SpeechRecognitionEventArgs>> recognizedCaptor = ArgumentCaptor.forClass(EventHandler.class);
            ArgumentCaptor<EventHandler<SessionEventArgs>> sessionStoppedCaptor = ArgumentCaptor.forClass(EventHandler.class);

            doNothing().when(recognizedSignal).addEventListener(recognizedCaptor.capture());
            doNothing().when(canceledSignal).addEventListener(any());
            doNothing().when(sessionStoppedSignal).addEventListener(sessionStoppedCaptor.capture());

            // java.util.concurrent.Future Mock 생성
            java.util.concurrent.Future<?> mockFuture = mock(java.util.concurrent.Future.class);
            when(mockFuture.get()).thenReturn(null);

            try (MockedConstruction<SpeechRecognizer> mockedRecognizer = mockConstruction(SpeechRecognizer.class, (mock, context) -> {
                ReflectionTestUtils.setField(mock, "recognized", recognizedSignal);
                ReflectionTestUtils.setField(mock, "canceled", canceledSignal);
                ReflectionTestUtils.setField(mock, "sessionStopped", sessionStoppedSignal);

                when(mock.startContinuousRecognitionAsync()).thenAnswer(invocation -> {
                    // 음성 인식 결과 이벤트 발생
                    SpeechRecognitionEventArgs mockEvent = mock(SpeechRecognitionEventArgs.class);
                    SpeechRecognitionResult mockResult = mock(SpeechRecognitionResult.class);
                    when(mockEvent.getResult()).thenReturn(mockResult);
                    when(mockResult.getReason()).thenReturn(ResultReason.RecognizedSpeech);
                    when(mockResult.getText()).thenReturn("테스트 음성 텍스트");
                    recognizedCaptor.getValue().onEvent(mock, mockEvent);

                    // 세션 종료 이벤트 발생 (세마포어 release)
                    SessionEventArgs mockSessionEvent = mock(SessionEventArgs.class);
                    sessionStoppedCaptor.getValue().onEvent(mock, mockSessionEvent);

                    return mockFuture;
                });

                when(mock.stopContinuousRecognitionAsync()).thenAnswer(invocation -> mockFuture);
            })) {

                // When
                String result = azureSpeechClient.speechToText(audioFile);

                // Then
                assertEquals("테스트 음성 텍스트", result);
            }
        }
    }

    @Test
    @DisplayName("STT 연속 인식 도중 에러(Canceled) 발생 시 RuntimeException 예외 처리 검증")
    void speechToText_Exception() throws Exception {
        // Given
        MultipartFile audioFile = new MockMultipartFile("audio", "test.wav", "audio/wav", new byte[]{1, 2, 3});

        try (MockedStatic<SpeechConfig> mockedSpeechConfig = mockStatic(SpeechConfig.class);
             MockedStatic<AudioConfig> mockedAudioConfig = mockStatic(AudioConfig.class)) {

            SpeechConfig mockSpeechConfig = mock(SpeechConfig.class);
            AudioConfig mockAudioConfig = mock(AudioConfig.class);

            mockedSpeechConfig.when(() -> SpeechConfig.fromSubscription("mock-key", "mock-region")).thenReturn(mockSpeechConfig);
            mockedAudioConfig.when(() -> AudioConfig.fromWavFileInput(anyString())).thenReturn(mockAudioConfig);

            EventHandlerImpl<SpeechRecognitionEventArgs> recognizedSignal = mock(EventHandlerImpl.class);
            EventHandlerImpl<SpeechRecognitionCanceledEventArgs> canceledSignal = mock(EventHandlerImpl.class);
            EventHandlerImpl<SessionEventArgs> sessionStoppedSignal = mock(EventHandlerImpl.class);

            ArgumentCaptor<EventHandler<SpeechRecognitionCanceledEventArgs>> canceledCaptor = ArgumentCaptor.forClass(EventHandler.class);

            doNothing().when(recognizedSignal).addEventListener(any());
            doNothing().when(canceledSignal).addEventListener(canceledCaptor.capture());
            doNothing().when(sessionStoppedSignal).addEventListener(any());

            java.util.concurrent.Future<?> mockFuture = mock(java.util.concurrent.Future.class);
            when(mockFuture.get()).thenReturn(null);

            try (MockedConstruction<SpeechRecognizer> mockedRecognizer = mockConstruction(SpeechRecognizer.class, (mock, context) -> {
                ReflectionTestUtils.setField(mock, "recognized", recognizedSignal);
                ReflectionTestUtils.setField(mock, "canceled", canceledSignal);
                ReflectionTestUtils.setField(mock, "sessionStopped", sessionStoppedSignal);

                when(mock.startContinuousRecognitionAsync()).thenAnswer(invocation -> {
                    // 에러 발생 취소 이벤트 발생
                    SpeechRecognitionCanceledEventArgs mockCancelEvent = mock(SpeechRecognitionCanceledEventArgs.class);
                    when(mockCancelEvent.getReason()).thenReturn(CancellationReason.Error);
                    when(mockCancelEvent.getErrorDetails()).thenReturn("Azure Speech Service connection failed");
                    canceledCaptor.getValue().onEvent(mock, mockCancelEvent);

                    return mockFuture;
                });

                when(mock.stopContinuousRecognitionAsync()).thenAnswer(invocation -> mockFuture);
            })) {

                // When & Then
                RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                    azureSpeechClient.speechToText(audioFile);
                });

                assertTrue(exception.getMessage().contains("STT Error: Azure Speech Service connection failed"));
            }
        }
    }
}
