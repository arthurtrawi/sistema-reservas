import repository.ClienteRepository;
import repository.RecursoRepository;
import repository.ReservaRepository;
import service.ClienteService;
import service.RecursoService;
import service.ReservaService;
import view.JanelaPrincipal;
import view.componentes.Tema;

import javax.swing.SwingUtilities;

/**
 * Ponto de entrada da aplicação desktop.
 *
 * 1. Monta o back-end: repositórios e serviços, ligados pelo construtor
 *    (injeção de dependência manual).
 * 2. Inicia a interface gráfica na Event Dispatch Thread (EDT), a única
 *    thread em que o Swing permite criar e alterar componentes.
 *
 * Os cenários de teste do back-end estão em teste.CenariosBackend.
 */
public class Main {

    public static void main(String[] args) {
        // Suavização de fontes; precisa ser definida antes de o AWT ser carregado.
        System.setProperty("awt.useSystemAAFontSettings", "on");

        ReservaRepository reservaRepository = new ReservaRepository();
        ClienteService clienteService = new ClienteService(new ClienteRepository(), reservaRepository);
        RecursoService recursoService = new RecursoService(new RecursoRepository(), reservaRepository);
        ReservaService reservaService = new ReservaService(reservaRepository, clienteService, recursoService);

        SwingUtilities.invokeLater(() -> {
            Tema.aplicar();
            new JanelaPrincipal(clienteService, recursoService, reservaService).setVisible(true);
        });
    }
}
