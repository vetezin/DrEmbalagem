package br.com.doutorembalagem.usuario.dto;

import br.com.doutorembalagem.erro.RequisicaoInvalidaException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public record PaginaResponse<T>(int page, long total, List<T> itens) {
    public static <T> PaginaResponse<T> de(Page<T> page) {
        return new PaginaResponse<>(page.getNumber(), page.getTotalElements(), page.getContent());
    }
    public static PageRequest pedido(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new RequisicaoInvalidaException("Paginacao invalida.");
        }
        return PageRequest.of(page, size, Sort.by("id"));
    }
}
