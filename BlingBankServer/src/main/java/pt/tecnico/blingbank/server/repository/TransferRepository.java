package pt.tecnico.blingbank.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;
import pt.tecnico.blingbank.server.domain.Transfer;

@Repository
@Transactional
public interface TransferRepository extends JpaRepository<Transfer, Integer> {

}
