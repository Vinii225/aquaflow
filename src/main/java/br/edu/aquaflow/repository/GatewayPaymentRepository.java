package br.edu.aquaflow.repository;

import br.edu.aquaflow.domain.GatewayPayment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface GatewayPaymentRepository extends JpaRepository<GatewayPayment, UUID> {

    Optional<GatewayPayment> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Trava a linha até o fim da transação: o retorno do aluno (back_url) e o webhook podem
     * chegar ao mesmo tempo, e só um deve lançar a entrada no caixa.
     * Sem filtro de escola de propósito: o webhook chega sem usuário logado e a escola vem da linha.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from GatewayPayment p where p.id = :id")
    Optional<GatewayPayment> lockById(UUID id);
}
