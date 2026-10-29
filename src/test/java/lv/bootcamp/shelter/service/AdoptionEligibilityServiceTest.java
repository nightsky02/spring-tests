package lv.bootcamp.shelter.service;

import lv.bootcamp.shelter.audit.AuditLogger;
import lv.bootcamp.shelter.client.NotificationClient;
import lv.bootcamp.shelter.model.*;
import lv.bootcamp.shelter.repository.AdopterRepository;
import lv.bootcamp.shelter.repository.AnimalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.Optional;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AdoptionEligibilityServiceTest {
    @Mock
    AdopterRepository adopterRepository;

    @Mock
    AnimalRepository animalRepository;

    @Mock
    NotificationClient client;

    @Mock
    AuditLogger logger;

    @InjectMocks
    AdoptionEligibilityService service;

    @Test
    void get_ShouldRejectEligibilityIfAdopterNotExists(){
        when(adopterRepository.findById(2L)).thenReturn(Optional.empty());

        AdoptionResult result = this.service.evaluateAdoption(2L, 1L);

        assertThat(result.approved()).isFalse();
        assertThat(result.reason()).isEqualTo(RejectionReasons.ADOPTER_NOT_FOUND);
    }


    @Test
    void get_ShouldRejectEligibilityIfAnimalNotExists(){
        when(animalRepository.findById(1L)).thenReturn(Optional.empty());
        when(adopterRepository.findById(2L)).thenReturn(Optional.of(new Adopter()));

        AdoptionResult result = this.service.evaluateAdoption(2L, 1L);

        assertThat(result.approved()).isFalse();
        assertThat(result.reason()).isEqualTo(RejectionReasons.ANIMAL_NOT_FOUND);
    }

    @Test
    void get_ShouldRejectEligibilityIfAnimalNotAvailable(){
        Animal animal = new Animal();
        animal.setStatus(AnimalStatus.ADOPTED);

        when(animalRepository.findById(1L)).thenReturn(Optional.of(animal));
        when(adopterRepository.findById(2L)).thenReturn(Optional.of(new Adopter()));

        AdoptionResult result = this.service.evaluateAdoption(2L, 1L);

        assertThat(result.approved()).isFalse();
        assertThat(result.reason()).isEqualTo(RejectionReasons.ANIMAL_NOT_AVAILABLE);
    }

    @Test
    void get_ShouldRejectEligibilityIfUnderageAdopter(){
        Animal animal = new Animal();
        animal.setStatus(AnimalStatus.AVAILABLE);

        Adopter adopter = new Adopter();
        adopter.setAge(15);

        when(animalRepository.findById(1L)).thenReturn(Optional.of(animal));
        when(adopterRepository.findById(2L)).thenReturn(Optional.of(adopter));

        AdoptionResult result = this.service.evaluateAdoption(2L, 1L);

        assertThat(result.approved()).isFalse();
        assertThat(result.reason()).isEqualTo(RejectionReasons.UNDERAGE);
    }

    @Test
    void get_ShouldRejectEligibilityIfPetLimitReached(){
        Animal animal = new Animal();
        animal.setStatus(AnimalStatus.AVAILABLE);

        Adopter adopter = new Adopter();
        adopter.setAge(18);

        when(animalRepository.findById(1L)).thenReturn(Optional.of(animal));
        when(adopterRepository.findById(2L)).thenReturn(Optional.of(adopter));


        // Firstly, check without large property
        adopter.setLargeProperty(false);
        adopter.setCurrentPetCount(4); // higher than 3

        AdoptionResult result = this.service.evaluateAdoption(2L, 1L);

        assertThat(result.approved()).isFalse();
        assertThat(result.reason()).isEqualTo(RejectionReasons.PET_LIMIT_REACHED);

        // Then enable large property

        adopter.setLargeProperty(true);
        adopter.setCurrentPetCount(6); // higher than 5

        result = this.service.evaluateAdoption(2L, 1L);
        assertThat(result.approved()).isFalse();
        assertThat(result.reason()).isEqualTo(RejectionReasons.PET_LIMIT_REACHED);
    }


    @Test
    void get_ShouldRejectEligibilityIfNoExoticPermit(){
        Animal animal = new Animal();
        animal.setStatus(AnimalStatus.AVAILABLE);
        animal.setType(AnimalType.BIRD);

        Adopter adopter = new Adopter();
        adopter.setAge(18);
        adopter.setExoticPermit(false);

        when(animalRepository.findById(1L)).thenReturn(Optional.of(animal));
        when(adopterRepository.findById(2L)).thenReturn(Optional.of(adopter));

        AdoptionResult result = this.service.evaluateAdoption(2L, 1L);

        assertThat(result.approved()).isFalse();
        assertThat(result.reason()).isEqualTo(RejectionReasons.EXOTIC_PERMIT_REQUIRED);
    }

    @Test
    void get_ShouldReturnScore10(){
        Animal animal = new Animal();
        animal.setStatus(AnimalStatus.AVAILABLE);
        animal.setType(AnimalType.CAT);
        animal.setAge(2);

        Adopter adopter = new Adopter();
        adopter.setAge(18);
        adopter.setLargeProperty(false);
        adopter.setPreviousAdoptions(2);
        adopter.setCurrentPetCount(0);

        when(animalRepository.findById(1L)).thenReturn(Optional.of(animal));
        when(adopterRepository.findById(2L)).thenReturn(Optional.of(adopter));

        AdoptionResult result = this.service.evaluateAdoption(2L, 1L);

        assertThat(result.approved()).isTrue();
        assertThat(result.priorityScore()).isEqualTo(10);
    }

    @Test
    void get_ShouldCalculateScoreWithLargerProperty(){
        Animal animal = new Animal();
        animal.setStatus(AnimalStatus.AVAILABLE);
        animal.setType(AnimalType.CAT);
        animal.setAge(2);

        Adopter adopter = new Adopter();
        adopter.setAge(18);
        adopter.setLargeProperty(true);
        adopter.setPreviousAdoptions(2);
        adopter.setCurrentPetCount(0);

        when(animalRepository.findById(1L)).thenReturn(Optional.of(animal));
        when(adopterRepository.findById(2L)).thenReturn(Optional.of(adopter));

        AdoptionResult result = this.service.evaluateAdoption(2L, 1L);

        assertThat(result.approved()).isTrue();
        assertThat(result.priorityScore()).isEqualTo(25);
        // 10 + 15 - (0 * 2) = 25
    }

    @Test
    void get_ShouldCalculateScoreWithSeniorAnimal(){
        Animal animal = new Animal();
        animal.setStatus(AnimalStatus.AVAILABLE);
        animal.setType(AnimalType.CAT);
        animal.setAge(8);

        Adopter adopter = new Adopter();
        adopter.setAge(18);
        adopter.setLargeProperty(true);
        adopter.setPreviousAdoptions(2);
        adopter.setCurrentPetCount(0);

        when(animalRepository.findById(1L)).thenReturn(Optional.of(animal));
        when(adopterRepository.findById(2L)).thenReturn(Optional.of(adopter));

        AdoptionResult result = this.service.evaluateAdoption(2L, 1L);

        assertThat(result.approved()).isTrue();
        assertThat(result.priorityScore()).isEqualTo(45);
        // 10 + 15 + 20 - (0 * 2) = 45
    }

    @Test
    void get_ShouldCalculateScoreWithPreviousAdoptions(){
        Animal animal = new Animal();
        animal.setStatus(AnimalStatus.AVAILABLE);
        animal.setType(AnimalType.CAT);
        animal.setAge(8);

        Adopter adopter = new Adopter();
        adopter.setAge(18);
        adopter.setLargeProperty(true);
        adopter.setPreviousAdoptions(5);
        adopter.setCurrentPetCount(0);

        when(animalRepository.findById(1L)).thenReturn(Optional.of(animal));
        when(adopterRepository.findById(2L)).thenReturn(Optional.of(adopter));

        AdoptionResult result = this.service.evaluateAdoption(2L, 1L);

        assertThat(result.approved()).isTrue();
        assertThat(result.priorityScore()).isEqualTo(50);

        // 10 + 15 + 5 + 20 - (0 * 2) = 50
    }

    @Test
    void get_ShouldCalculateScoreWithCurrentPetCount(){
        Animal animal = new Animal();
        animal.setStatus(AnimalStatus.AVAILABLE);
        animal.setType(AnimalType.CAT);
        animal.setAge(8);

        Adopter adopter = new Adopter();
        adopter.setAge(18);
        adopter.setLargeProperty(true);
        adopter.setPreviousAdoptions(5);
        adopter.setCurrentPetCount(3);

        when(animalRepository.findById(1L)).thenReturn(Optional.of(animal));
        when(adopterRepository.findById(2L)).thenReturn(Optional.of(adopter));

        AdoptionResult result = this.service.evaluateAdoption(2L, 1L);

        assertThat(result.approved()).isTrue();
        assertThat(result.priorityScore()).isEqualTo(44);

        // 10 + 15 + 5 + 20 - (3 * 2) = 44
    }
}
