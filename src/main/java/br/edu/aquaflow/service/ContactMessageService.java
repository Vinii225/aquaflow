package br.edu.aquaflow.service;

import br.edu.aquaflow.domain.ContactMessage;
import br.edu.aquaflow.domain.enums.ContactMessageStatus;
import br.edu.aquaflow.repository.ContactMessageRepository;
import br.edu.aquaflow.repository.PlanRepository;
import br.edu.aquaflow.repository.UserRepository;
import br.edu.aquaflow.web.dto.ContactForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ContactMessageService {

    public static final List<String> FORWARD_TARGETS = List.of("Dono da escola", "Secretaria");

    private final ContactMessageRepository contactMessageRepository;
    private final PlanRepository planRepository;
    private final UserRepository userRepository;
    private final TenantContext tenantContext;

    public ContactMessageService(ContactMessageRepository contactMessageRepository,
                                  PlanRepository planRepository,
                                  UserRepository userRepository,
                                  TenantContext tenantContext) {
        this.contactMessageRepository = contactMessageRepository;
        this.planRepository = planRepository;
        this.userRepository = userRepository;
        this.tenantContext = tenantContext;
    }

    @Transactional
    public void send(ContactForm form, UUID senderId) {
        var tenant = tenantContext.getTenant();

        ContactMessage contact = new ContactMessage();
        contact.setTenant(tenant);
        if (form.getPlanId() != null) {
            planRepository.findById(form.getPlanId())
                    .filter(p -> p.getTenant().getId().equals(tenant.getId()))
                    .ifPresent(contact::setPlan);
        }
        if (senderId != null) {
            userRepository.findById(senderId).ifPresent(contact::setSender);
        }
        contact.setName(form.getName().trim());
        contact.setEmail(form.getEmail().trim());
        contact.setPhone(form.getPhone() == null || form.getPhone().isBlank() ? null : form.getPhone().trim());
        contact.setMessage(form.getMessage().trim());
        contactMessageRepository.save(contact);
    }

    @Transactional
    public void forward(UUID messageId, String forwardedTo, String note, UUID forwardedById) {
        if (!FORWARD_TARGETS.contains(forwardedTo)) {
            throw new IllegalArgumentException("Destino de encaminhamento inválido");
        }
        ContactMessage contact = find(messageId);
        contact.setStatus(ContactMessageStatus.FORWARDED);
        contact.setForwardedTo(forwardedTo);
        contact.setForwardNote(note == null || note.isBlank() ? null : note.trim());
        contact.setForwardedBy(userRepository.findById(forwardedById)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado")));
        contact.setForwardedAt(Instant.now());
        contact.setUpdatedAt(Instant.now());
    }

    @Transactional
    public void changeStatus(UUID messageId, ContactMessageStatus status) {
        ContactMessage contact = find(messageId);
        contact.setStatus(status);
        contact.setUpdatedAt(Instant.now());
    }

    public ContactMessage find(UUID messageId) {
        return contactMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Mensagem não encontrada"));
    }
}
