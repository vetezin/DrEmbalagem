package br.com.doutorembalagem.usuario.controller;

import br.com.doutorembalagem.autenticacao.service.UsuarioDetalhes;
import br.com.doutorembalagem.usuario.dto.*;
import br.com.doutorembalagem.usuario.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class UsuarioController {
    private final UsuarioService service;

    @GetMapping("/api/usuarios")
    public PaginaResponse<UsuarioResponse> listar(@AuthenticationPrincipal UsuarioDetalhes ator,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.listar(ator.id(), page, size);
    }

    @PostMapping("/api/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse criar(@AuthenticationPrincipal UsuarioDetalhes ator,
            @Valid @RequestBody CriarUsuarioRequest pedido) {
        return service.criar(ator.id(), pedido);
    }

    @PutMapping("/api/usuarios/{id}/permissoes")
    public UsuarioResponse permissoes(@AuthenticationPrincipal UsuarioDetalhes ator, @PathVariable Long id,
            @Valid @RequestBody AlterarPermissoesRequest pedido) {
        return service.alterarPermissoes(ator.id(), id, pedido);
    }

    @GetMapping("/api/permissoes")
    public List<String> catalogo(@AuthenticationPrincipal UsuarioDetalhes ator) {
        return service.catalogo(ator.id());
    }
}
