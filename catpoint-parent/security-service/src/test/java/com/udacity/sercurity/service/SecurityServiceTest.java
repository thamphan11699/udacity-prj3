package com.udacity.sercurity.service;

import com.udacity.imageservice.ImageService;
import com.udacity.sercurity.application.StatusListener;
import com.udacity.sercurity.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

  private static final String SENSOR_NAME = "Test";

  private static final Integer SENSOR_SIZE = 5;

  private static final Integer IMAGE_SIZE = 128;

  @Mock private StatusListener statusListener;

  @Mock private ImageService imageService;

  @Mock private SecurityRepository securityRepository;

  @InjectMocks private SecurityService securityService;

  private Sensor sensor;

  private Sensor getNewSensor() {
    return new Sensor(SENSOR_NAME, SensorType.DOOR);
  }

  private Set<Sensor> getAllSensors(boolean status) {
    Set<Sensor> sensors = new HashSet<>();
    for (int i = 0; i < SecurityServiceTest.SENSOR_SIZE; i++) {
      sensors.add(new Sensor(SENSOR_NAME, SensorType.DOOR));
    }
    sensors.forEach(sensor -> sensor.setActive(status));

    return sensors;
  }

  @BeforeEach
  void init() {
    securityService = new SecurityService(securityRepository, imageService);
    sensor = getNewSensor();
  }

  @Test
  @DisplayName("Test 1")
  void ifAlarmIsArmedAndASensorBecomesActivatedThenPutTheSystemIntoPendingAlarmStatus() {
    when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
    when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
    securityService.changeSensorActivationStatus(sensor, true);

    verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
  }

  @Test
  @DisplayName("Test 2")
  void
      ifAlarmIsArmedAndASensorBecomesActivatedAndTheSystemIsAlreadyPendingAlarmThenSetTheAlarmStatusToAlarm() {
    when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
    when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
    securityService.changeSensorActivationStatus(sensor, true);

    verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
  }

  @Test
  @DisplayName("Test 3")
  void ifPendingAlarmAndAllSensorsAreInactiveThenReturnToNoAlarmState() {
    when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
    sensor.setActive(false);
    securityService.changeSensorActivationStatus(sensor);

    verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
  }

  @ParameterizedTest
  @DisplayName("Test 4")
  @ValueSource(booleans = {true, false})
  void ifAlarmIsActiveThenChangeInSensorStateShouldNotAffectTheAlarmState(boolean status) {
    when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
    securityService.changeSensorActivationStatus(sensor, status);

    verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
  }

  @Test
  @DisplayName("Test 5")
  void ifASensorIsActivatedWhileAlreadyActiveAndTheSystemIsInPendingStateTheChangeItToAlarmState() {
    when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
    sensor.setActive(true);
    securityService.changeSensorActivationStatus(sensor, true);

    verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
  }

  @ParameterizedTest
  @DisplayName("Test 6")
  @EnumSource(
      value = AlarmStatus.class,
      names = {"NO_ALARM", "PENDING_ALARM", "ALARM"})
  void ifASensorIsDeactivatedWhileAlreadyInactiveThenMakeNoChangesToTheAlarmState(
      AlarmStatus status) {
    when(securityRepository.getAlarmStatus()).thenReturn(status);
    sensor.setActive(false);
    securityService.changeSensorActivationStatus(sensor, false);

    verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
  }

  @Test
  @DisplayName("Test 7")
  void
      ifTheImageServiceIdentifiesAnImageContainingACatWhileTheSystemIsArmedHomeThenPutTheSystemIntoAlarmStatus() {
    BufferedImage catImage = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_RGB);
    when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
    when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
    securityService.processImage(catImage);

    verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
  }

  @Test
  @DisplayName("Test 8")
  void
      ifTheImageServiceIdentifiesAnImageThatDoesNotContainACatThenChangeTheStatusToNoAlarmAsLongAsTheSensorsAreNotActive() {
    lenient().when(securityRepository.getSensors()).thenReturn(getAllSensors(false));
    when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);
    securityService.processImage(mock(BufferedImage.class));

    verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
  }

  @Test
  @DisplayName("Test 9")
  void ifTheSystemIsDisarmedThenSetTheStatusToNoAlarm() {
    securityService.setArmingStatus(ArmingStatus.DISARMED);

    verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
  }

  @ParameterizedTest
  @DisplayName("Test 10")
  @EnumSource(
      value = ArmingStatus.class,
      names = {"ARMED_HOME", "ARMED_AWAY"})
  void ifTheSystemIsArmedThenResetAllSensorsToInactive(ArmingStatus status) {
    when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
    when(securityRepository.getSensors()).thenReturn(getAllSensors(true));
    securityService.setArmingStatus(status);

    securityService
        .getSensors()
        .forEach(
            sensor -> {
              assertFalse(sensor.getActive());
            });
  }

  @Test
  @DisplayName("Test 11")
  void ifTheSystemIsArmedHomeWhileTheCameraShowsACatThenSetTheAlarmStatusToAlarm() {
    BufferedImage catImage = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_RGB);
    when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
    when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
    securityService.processImage(catImage);
    securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

    verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
  }

  @Test
  @DisplayName("Add status Listener")
  void addStatusListener() {
    securityService.addStatusListener(statusListener);
  }

  @Test
  @DisplayName("Remove status Listener")
  void removeStatusListener() {
    securityService.removeStatusListener(statusListener);
  }

  @Test
  @DisplayName("Add Sensor")
  void addSensor() {
    securityService.addSensor(sensor);
  }

  @Test
  @DisplayName("Remove Sensor")
  void removeSensor() {
    securityService.removeSensor(sensor);
  }

  @ParameterizedTest
  @DisplayName("Test 12")
  @EnumSource(
      value = AlarmStatus.class,
      names = {"NO_ALARM", "PENDING_ALARM"})
  void ifTheSystemIsDisarmedAndSensorIsActivatedThenNotSetArmingState(AlarmStatus status) {
    when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
    when(securityRepository.getAlarmStatus()).thenReturn(status);
    securityService.changeSensorActivationStatus(sensor, true);

    verify(securityRepository, never()).setArmingStatus(ArmingStatus.DISARMED);
  }

  @Test
  @DisplayName("Test 13")
  void ifTheAlarmStatusIsAlarmAndTheSystemIsDisarmedThenSetAlarmStatusToPending() {
    when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
    when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
    securityService.changeSensorActivationStatus(sensor);

    verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
  }
}
