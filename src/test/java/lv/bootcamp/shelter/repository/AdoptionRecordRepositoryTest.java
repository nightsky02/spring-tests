package lv.bootcamp.shelter.repository;

import lv.bootcamp.shelter.model.AdoptionRecord;
import lv.bootcamp.shelter.model.Animal;
import lv.bootcamp.shelter.model.AnimalStatus;
import lv.bootcamp.shelter.model.AnimalType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class AdoptionRecordRepositoryTest {

    @Autowired
    private AdoptionRecordRepository recordRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByRecentAdoptions_ShouldProvideFilteredData(){
        Animal firstAnimal = createAnimal("Doggy", AnimalType.DOG, "dog");
        Animal secondAnimal = createAnimal("Super cat", AnimalType.CAT, "cat");
        Animal thirdAnimal = createAnimal("Rex", AnimalType.DOG, "dog");

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

        AdoptionRecord firstRecord = createAdoptionRecord(firstAnimal,
                "alex@gmail.com", LocalDateTime.parse("26.10.2026 14:00:00", formatter));

        AdoptionRecord secondRecord = createAdoptionRecord(secondAnimal,
                "alex@gmail.com", LocalDateTime.parse("28.10.2026 15:57:00", formatter));

        AdoptionRecord thirdRecord = createAdoptionRecord(thirdAnimal,
                "anna@gmail.com", LocalDateTime.parse("14.10.2026 10:00:00", formatter));

        // adding animals
        entityManager.persist(firstAnimal);
        entityManager.persist(secondAnimal);
        entityManager.persist(thirdAnimal);
        entityManager.flush();

        // adding records
        entityManager.persist(firstRecord);
        entityManager.persist(secondRecord);
        entityManager.persist(thirdRecord);
        entityManager.flush();

        // calling repository function
        List<AdoptionRecord> records = this.recordRepository
                .findRecentAdoptionsByAnimalType(AnimalType.DOG, LocalDateTime.parse("13.10.2026 00:00:00", formatter));

        // asserting that size is two and all the animals stored are dogs.
        assertThat(records).hasSize(2);
        assertThat(records.getFirst().getAnimal().getType()).isEqualTo(AnimalType.DOG);
        assertThat(records.get(1).getAnimal().getType()).isEqualTo(AnimalType.DOG);
    }
    private Animal createAnimal(String name, AnimalType type, String breed){
        Animal animal = new Animal();
        animal.setName(name);
        animal.setType(type);
        animal.setBreed(breed);
        animal.setStatus(AnimalStatus.AVAILABLE);
        return animal;
    }
    private AdoptionRecord createAdoptionRecord(Animal animal, String adopterEmail, LocalDateTime adoptedAt){
        AdoptionRecord record = new AdoptionRecord();
        record.setAdoptedAt(adoptedAt);
        record.setAnimal(animal);
        record.setAdopterEmail(adopterEmail);
        return record;
    }
}
