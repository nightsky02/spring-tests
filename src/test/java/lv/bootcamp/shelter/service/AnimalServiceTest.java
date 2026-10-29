package lv.bootcamp.shelter.service;

import lv.bootcamp.shelter.client.NotificationClient;
import lv.bootcamp.shelter.dto.AdoptionRequest;
import lv.bootcamp.shelter.dto.AnimalCreateRequest;
import lv.bootcamp.shelter.dto.AnimalResponse;
import lv.bootcamp.shelter.model.Animal;
import lv.bootcamp.shelter.model.AnimalStatus;
import lv.bootcamp.shelter.model.AnimalType;
import lv.bootcamp.shelter.repository.AnimalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class AnimalServiceTest {

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private NotificationClient notificationClient;

    @InjectMocks
    private AnimalService animalService;

    @Captor
    private ArgumentCaptor<List<Long>> reversedAnimalsId;

    @Test
    void create_shouldSaveAnimalWithAvailableStatus() {
        Animal saved = new Animal();
        saved.setId(1L);
        saved.setName("Rex");
        saved.setType(AnimalType.DOG);
        saved.setStatus(AnimalStatus.AVAILABLE);

        when(animalRepository.save(any(Animal.class))).thenReturn(saved);

        AnimalResponse response = animalService.create(
                new AnimalCreateRequest("Rex", AnimalType.DOG, "Labrador", 3, "Friendly", null));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Rex");
        assertThat(response.status()).isEqualTo(AnimalStatus.AVAILABLE);
    }

    @Test
    void findById_shouldThrowWhenAnimalNotFound() {
        when(animalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> animalService.findById(99L))
                .isInstanceOf(AnimalNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void adopt_shouldThrowWhenAnimalAlreadyAdopted() {
        Animal adopted = new Animal();
        adopted.setId(1L);
        adopted.setStatus(AnimalStatus.ADOPTED);

        when(animalRepository.findById(1L)).thenReturn(Optional.of(adopted));

        assertThatThrownBy(() -> animalService.adopt(
                new AdoptionRequest(1L, "Anna", "anna@example.com")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void adopt_shouldChangeStatusAndCallNotificationService(){
        Animal adopted = new Animal();
        adopted.setId(1L);
        adopted.setName("name");
        adopted.setStatus(AnimalStatus.AVAILABLE);

        when(animalRepository.findById(1L)).thenReturn(Optional.of(adopted));
        when(animalRepository.save(any(Animal.class))).thenReturn(adopted);

        animalService.adopt(new AdoptionRequest(1L, "name", "name@example.com"));

        assertThat(adopted.getStatus()).isEqualTo(AnimalStatus.ADOPTED);
        verify(this.notificationClient)
                .sendAdoptionNotification(1L, "name", "name@example.com");

    }

    @Test
    void multiplyReserveShouldCallExternalSystemCorrectly(){
        // create instances of animals and bind
        when(animalRepository.findAllById(any())).thenReturn(List.of(
                new Animal(1L, null, null, null, null, null, null, AnimalStatus.AVAILABLE),
                new Animal(2L, null, null, null, null, null, null, AnimalStatus.AVAILABLE)
        ));

        when(animalRepository.save(any(Animal.class)))
                .thenAnswer(ans -> ans.getArgument(0));

        animalService.reserveMultiple(List.of(1L, 2L));

        verify(notificationClient)
                .sendBulkStatusNotification(this.reversedAnimalsId.capture(), eq("RESERVED"));

        assertThat(this.reversedAnimalsId.getValue())
                .containsExactly(1L, 2L);
    }

    @Test
    void create_shouldNotAddAnimalWithTheSameChipId(){
        when(animalRepository.existsByMicrochipId("12345")).thenReturn(true);

        assertThatThrownBy(() -> animalService.create(new AnimalCreateRequest(
            "Super cat", AnimalType.CAT, "cat", 2, "no description", "12345"
        ))).isInstanceOf(IllegalStateException.class);

        verify(animalRepository, never()).save(any(Animal.class)); // any because we can not know the exact id of animal
    }
}
