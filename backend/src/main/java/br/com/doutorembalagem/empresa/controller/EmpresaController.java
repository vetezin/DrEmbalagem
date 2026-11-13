package br.com.doutorembalagem.empresa.controller;

import br.com.doutorembalagem.autenticacao.service.UsuarioDetalhes;
import br.com.doutorembalagem.empresa.dto.CriarEmpresaRequest;
import br.com.doutorembalagem.empresa.dto.EmpresaResponse;
import br.com.doutorembalagem.empresa.service.EmpresaService;
import br.com.doutorembalagem.usuario.dto.PaginaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/empresas")
@RequiredArgsConstructor
public class EmpresaController {
    private final EmpresaService service;

    @GetMapping
    public PaginaResponse<EmpresaResponse> listar(@AuthenticationPrincipal UsuarioDetalhes ator,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.listar(ator.id(), page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmpresaResponse criar(@AuthenticationPrincipal UsuarioDetalhes ator,
            @Valid @RequestBody CriarEmpresaRequest pedido) {
        return service.criar(ator.id(), pedido);
    }
}
