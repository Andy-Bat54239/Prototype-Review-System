package com.auca.prbs.repository;

import com.auca.prbs.entity.UserRole;
import com.auca.prbs.entity.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired UserRepository users;

    @Test
    void seedUser_alice_isPresent() {
        var alice = users.findByEmail("alice@university.ac.rw");

        assertThat(alice).isPresent();
        assertThat(alice.get().getRole()).isEqualTo(UserRole.STUDENT);
        assertThat(alice.get().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void seedUser_supervisor_hasSupervisorRole() {
        var supervisor = users.findByEmail("supervisor@university.ac.rw");

        assertThat(supervisor).isPresent();
        assertThat(supervisor.get().getRole()).isEqualTo(UserRole.SUPERVISOR);
    }

    @Test
    void seedUser_chidi_isInactive() {
        var chidi = users.findByEmail("chidi@university.ac.rw");

        assertThat(chidi).isPresent();
        assertThat(chidi.get().getStatus()).isEqualTo(UserStatus.INACTIVE);
    }

    @Test
    void unknownEmail_returnsEmpty() {
        assertThat(users.findByEmail("nobody@nowhere.example")).isEmpty();
    }
}
