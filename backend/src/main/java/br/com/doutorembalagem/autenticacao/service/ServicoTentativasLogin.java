package br.com.doutorembalagem.autenticacao.service;

import br.com.doutorembalagem.erro.LimiteTentativasException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;

/** Limita tentativas de login por conta e por IP (em memoria, por instancia). */
@Service
public class ServicoTentativasLogin {

    private static final int MAX_TENTATIVAS_CONTA = 5;
    private static final long BLOQUEIO_CONTA_MS = 15L * 60 * 1000;
    private static final int MAX_TENTATIVAS_IP = 20;
    private static final long JANELA_IP_MS = 60L * 1000;
    private static final long BLOQUEIO_IP_MS = 5L * 60 * 1000;

    private final ConcurrentMap<String, TentativaConta> contas = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, TentativaIp> ips = new ConcurrentHashMap<>();

    public void verificarIp(String ip) {
        if (ip == null || ip.isBlank()) return;
        TentativaIp t = ips.computeIfAbsent(ip, k -> new TentativaIp());
        long agora = System.currentTimeMillis();
        synchronized (t) {
            if (t.bloqueadoAte > agora) {
                throw new LimiteTentativasException("Muitas tentativas. Tente novamente em alguns minutos.");
            }
            t.timestamps.removeIf(ts -> agora - ts > JANELA_IP_MS);
            if (t.timestamps.size() >= MAX_TENTATIVAS_IP) {
                t.bloqueadoAte = agora + BLOQUEIO_IP_MS;
                throw new LimiteTentativasException("Muitas tentativas. Tente novamente em alguns minutos.");
            }
            t.timestamps.add(agora);
        }
    }

    public void verificarBloqueio(String chave) {
        TentativaConta t = contas.get(chave);
        if (t == null) return;
        long agora = System.currentTimeMillis();
        synchronized (t) {
            if (t.bloqueadoAte > agora) {
                throw new LimiteTentativasException("Muitas tentativas. Tente novamente em alguns minutos.");
            }
        }
    }

    public void registrarFalha(String chave) {
        TentativaConta t = contas.computeIfAbsent(chave, k -> new TentativaConta());
        long agora = System.currentTimeMillis();
        synchronized (t) {
            if (t.bloqueadoAte > agora) return;
            t.falhas++;
            if (t.falhas >= MAX_TENTATIVAS_CONTA) {
                t.bloqueadoAte = agora + BLOQUEIO_CONTA_MS;
                t.falhas = 0;
            }
        }
    }

    public void registrarSucesso(String chave) {
        contas.remove(chave);
    }

    private static final class TentativaConta {
        int falhas;
        long bloqueadoAte;
    }

    private static final class TentativaIp {
        final List<Long> timestamps = new ArrayList<>();
        long bloqueadoAte;
    }
}