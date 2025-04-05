package pt.tecnico.blingbank.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;
import pt.tecnico.blingbank.server.domain.AccountHolder;

@Repository
@Transactional
public interface AccountHolderRepository extends JpaRepository<AccountHolder, Integer> {

    AccountHolder findByUsername(String username);

}
