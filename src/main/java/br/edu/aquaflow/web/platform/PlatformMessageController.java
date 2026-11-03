package br.edu.aquaflow.web.platform;

import br.edu.aquaflow.domain.ContactMessage;
import br.edu.aquaflow.domain.User;
import br.edu.aquaflow.domain.enums.ContactMessageStatus;
import br.edu.aquaflow.domain.enums.MembershipStatus;
import br.edu.aquaflow.domain.enums.TenantUserRole;
import br.edu.aquaflow.repository.ContactMessageRepository;
import br.edu.aquaflow.repository.TenantUserRepository;
import br.edu.aquaflow.security.AquaflowUserDetails;
import br.edu.aquaflow.service.ContactMessageService;
import br.edu.aquaflow.web.dto.ForwardForm;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/plataforma/mensagens")
public class PlatformMessageController {

    private static final DateTimeFormatter RECEIVED_AT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final ContactMessageRepository contactMessageRepository;
    private final ContactMessageService contactMessageService;
    private final TenantUserRepository tenantUserRepository;

    public PlatformMessageController(ContactMessageRepository contactMessageRepository,
                                      ContactMessageService contactMessageService,
                                      TenantUserRepository tenantUserRepository) {
        this.contactMessageRepository = contactMessageRepository;
        this.contactMessageService = contactMessageService;
        this.tenantUserRepository = tenantUserRepository;
    }

    @GetMapping
    public String list(@RequestParam(required = false) ContactMessageStatus status,
                        @PageableDefault(size = 15) Pageable pageable,
                        Model model) {
        Page<ContactMessage> messages = status == null
                ? contactMessageRepository.findAllByOrderByCreatedAtDesc(pageable)
                : contactMessageRepository.findByStatusOrderByCreatedAtDesc(status, pageable);

        model.addAttribute("messages", messages);
        model.addAttribute("status", status);
        model.addAttribute("statuses", ContactMessageStatus.values());
        model.addAttribute("newCount", contactMessageRepository.countByStatus(ContactMessageStatus.NEW));
        return "platform/messages";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable UUID id, Model model) {
        ContactMessage contact = contactMessageService.find(id);

        var schoolAdmins = tenantUserRepository.findByTenantIdAndRole(contact.getTenant().getId(), TenantUserRole.SCHOOL_ADMIN).stream()
                .filter(tu -> tu.getStatus() == MembershipStatus.ACTIVE)
                .map(tu -> tu.getUser())
                .toList();

        model.addAttribute("contact", contact);
        model.addAttribute("planLabel", planLabel(contact));
        model.addAttribute("schoolAdmins", schoolAdmins);
        model.addAttribute("forwardMailto", forwardMailto(contact, schoolAdmins));
        model.addAttribute("forwardTargets", ContactMessageService.FORWARD_TARGETS);
        if (!model.containsAttribute("forwardForm")) {
            model.addAttribute("forwardForm", new ForwardForm());
        }
        return "platform/message-detail";
    }

    @PostMapping("/{id}/forward")
    public String forward(@PathVariable UUID id,
                           @Valid @ModelAttribute("forwardForm") ForwardForm form,
                           BindingResult bindingResult,
                           @AuthenticationPrincipal AquaflowUserDetails currentUser,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors() || !ContactMessageService.FORWARD_TARGETS.contains(form.getForwardedTo())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Selecione para quem a mensagem foi encaminhada.");
            return "redirect:/plataforma/mensagens/" + id;
        }

        contactMessageService.forward(id, form.getForwardedTo(), form.getNote(), currentUser.getUserId());
        redirectAttributes.addFlashAttribute("successMessage", "Mensagem marcada como encaminhada para " + form.getForwardedTo() + ".");
        return "redirect:/plataforma/mensagens/" + id;
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable UUID id,
                                @RequestParam ContactMessageStatus status,
                                RedirectAttributes redirectAttributes) {
        if (!Arrays.asList(ContactMessageStatus.NEW, ContactMessageStatus.ARCHIVED).contains(status)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Status inválido.");
            return "redirect:/plataforma/mensagens/" + id;
        }

        contactMessageService.changeStatus(id, status);
        redirectAttributes.addFlashAttribute("successMessage", "Mensagem marcada como " + status.getLabel().toLowerCase() + ".");
        return "redirect:/plataforma/mensagens/" + id;
    }

    private String planLabel(ContactMessage contact) {
        return contact.getPlan() != null ? "Plano " + contact.getPlan().getName() : "Dúvida geral sobre os planos";
    }

    private String forwardMailto(ContactMessage contact, List<User> recipients) {
        String to = recipients.stream().map(User::getEmail).collect(Collectors.joining(","));
        String subject = "Novo interessado: " + contact.getName() + " (" + planLabel(contact) + ")";
        String body = "Olá! Recebemos um novo contato pela página de planos do AquaFlow.\n\n"
                + "Nome: " + contact.getName() + "\n"
                + "E-mail: " + contact.getEmail() + "\n"
                + "Telefone: " + (contact.getPhone() != null ? contact.getPhone() : "-") + "\n"
                + "Interesse: " + planLabel(contact) + "\n"
                + "Recebida em: " + RECEIVED_AT.format(contact.getCreatedAt()) + "\n\n"
                + "Mensagem:\n" + contact.getMessage();
        return "mailto:" + to + "?subject=" + encode(subject) + "&body=" + encode(body);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
