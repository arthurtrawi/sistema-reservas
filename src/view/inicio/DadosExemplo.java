package view.inicio;

import model.Recurso;
import service.ClienteService;
import service.RecursoService;
import service.ReservaService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Carrega um conjunto de dados de demonstração (5 clientes, 6 recursos e
 * 8 reservas em diferentes status), para apresentar o sistema sem precisar
 * cadastrar tudo manualmente.
 *
 * Todos os registros são criados pelos SERVIÇOS — exatamente como se
 * tivessem sido digitados na tela —, passando pelas mesmas validações e
 * regras de negócio. Nada é inserido diretamente nos repositórios.
 *
 * As datas são relativas ao dia atual, para que os exemplos pareçam sempre
 * atuais: a maioria das reservas é para os próximos dias, e uma reserva
 * concluída fica no dia anterior.
 */
public class DadosExemplo {

    private final ClienteService clienteService;
    private final RecursoService recursoService;
    private final ReservaService reservaService;

    public DadosExemplo(ClienteService clienteService, RecursoService recursoService,
                        ReservaService reservaService) {
        this.clienteService = clienteService;
        this.recursoService = recursoService;
        this.reservaService = reservaService;
    }

    /** Os exemplos só podem ser carregados com o sistema vazio (evita CPFs duplicados). */
    public boolean podeCarregar() {
        return clienteService.listar().isEmpty()
                && recursoService.listar().isEmpty()
                && reservaService.listar().isEmpty();
    }

    public void carregar() {
        cadastrarClientes();
        Recurso[] recursos = cadastrarRecursos();
        criarReservas(recursos);
    }

    private void cadastrarClientes() {
        clienteService.cadastrar("111.444.777-35", "Ana Souza", "ana.souza@email.com", "(11) 91111-1111");
        clienteService.cadastrar("222.555.888-46", "Bruno Lima", "bruno.lima@email.com", "(11) 92222-2222");
        clienteService.cadastrar("333.666.999-57", "Carla Mendes", "carla.mendes@email.com", "(21) 93333-3333");
        clienteService.cadastrar("444.777.000-68", "Diego Ramos", "diego.ramos@email.com", "(31) 94444-4444");
        clienteService.cadastrar("555.888.111-79", "Elisa Martins", "elisa.martins@email.com", "(41) 95555-5555");
    }

    private Recurso[] cadastrarRecursos() {
        return new Recurso[] {
            recursoService.cadastrar("Sala 101", "Sala de reunião", "Capacidade para 8 pessoas, TV 55\""),
            recursoService.cadastrar("Sala 202", "Sala de reunião", "Capacidade para 20 pessoas"),
            recursoService.cadastrar("Auditório", "Espaço para eventos", "120 lugares, som e projeção"),
            recursoService.cadastrar("Projetor Epson", "Equipamento", "Projetor portátil com cabo HDMI"),
            recursoService.cadastrar("Laboratório de Informática", "Laboratório", "30 computadores"),
            recursoService.cadastrar("Quadra Poliesportiva", "Espaço esportivo", "Quadra coberta")
        };
    }

    private void criarReservas(Recurso[] r) {
        LocalDate amanha = LocalDate.now().plusDays(1);
        LocalDate ontem = LocalDate.now().minusDays(1);

        reservar("11144477735", r[0], amanha, 9, 11);                 // Ana     - Sala 101
        reservar("22255588846", r[0], amanha, 11, 12);                // Bruno   - Sala 101 (logo em seguida)
        reservar("33366699957", r[2], amanha, 14, 18);                // Carla   - Auditório
        reservar("44477700068", r[3], amanha, 9, 12);                 // Diego   - Projetor
        reservar("55588811179", r[4], amanha.plusDays(1), 8, 12);     // Elisa   - Laboratório
        reservar("22255588846", r[5], amanha.plusDays(2), 19, 21);    // Bruno   - Quadra

        int cancelada = reservar("11144477735", r[1], amanha, 10, 11);  // Ana   - Sala 202
        reservaService.cancelar(cancelada);

        int concluida = reservar("33366699957", r[0], ontem, 15, 17);   // Carla - Sala 101 (ontem)
        reservaService.concluir(concluida);
    }

    private int reservar(String cpf, Recurso recurso, LocalDate dia, int horaInicio, int horaFim) {
        LocalDateTime inicio = dia.atTime(LocalTime.of(horaInicio, 0));
        LocalDateTime fim = dia.atTime(LocalTime.of(horaFim, 0));
        return reservaService.criar(cpf, recurso.getId(), inicio, fim).getId();
    }
}
