package com.upc.edufinservice.assessment.infrastructure.seeders;

import com.upc.edufinservice.assessment.domain.model.experimental.ExperimentalOption;
import com.upc.edufinservice.assessment.domain.model.experimental.ExperimentalQuestion;
import com.upc.edufinservice.assessment.infrastructure.persistence.jpa.repositories.ExperimentalQuestionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(50)
public class ExperimentalAssessmentSeeder implements CommandLineRunner {
    private final ExperimentalQuestionRepository questionRepository;

    public ExperimentalAssessmentSeeder(ExperimentalQuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seed("PRE_Q01", 1, 1, "Ingresos y presupuesto", "Sueldo bruto vs. neto",
                "Una empresa emite la boleta de pago mensual de Juana correspondiente al mes de julio. El documento detalla: Salario bruto: S/ 2,800; Deducciones de ley (aportes previsionales e impuestos): S/ 300; Salario neto: S/ 2,500; Salario bruto acumulado del año: S/ 19,600. ¿Cuánto dinero deposita efectivamente la empresa en la cuenta bancaria de Juana a fin de mes?",
                "PISA 2012 - PF067Q01 (Nómina)", 1,
                new Opt("A", "S/ 300", false), new Opt("B", "S/ 2,500", true), new Opt("C", "S/ 2,800", false), new Opt("D", "S/ 19,600", false), new Opt("E", "No lo sé", false));

        seed("PRE_Q02", 2, 1, "Ingresos y presupuesto", "Control presupuestal",
                "¿Para cuál de las siguientes funciones NO puede utilizarse un presupuesto familiar o personal?",
                "P-Fin 8 - Q2 (Consuming / GFLEC)", 4,
                new Opt("A", "Para registrar y hacer el seguimiento del patrimonio o activos financieros acumulados (inmuebles, autos, cuentas de inversión)", true),
                new Opt("B", "Para planificar y priorizar los gastos necesarios y obligatorios del mes", false),
                new Opt("C", "Para planificar los gastos discrecionales o de entretenimiento personal", false),
                new Opt("D", "No lo sé", false));

        seed("PRE_Q03", 3, 2, "Ahorro", "Inflación y poder adquisitivo",
                "Alicia mantiene S/ 1,000 en una cuenta de ahorros que genera una tasa de rentabilidad del 2% al año. Durante ese mismo año, la tasa de inflación en el país fue del 3%. ¿Cuál de las siguientes afirmaciones es correcta respecto a su dinero al cabo de ese año?",
                "P-Fin 8 - Q3 / OCDE-INFE - QK3", 10,
                new Opt("A", "Alicia podrá comprar menos cosas con su dinero que al inicio del año", true), new Opt("B", "Alicia podrá comprar más cosas con su dinero que al inicio del año", false), new Opt("C", "Su poder adquisitivo se mantendrá exactamente igual", false), new Opt("D", "No lo sé", false));

        seed("PRE_Q04", 4, 2, "Ahorro", "Interés simple en cuentas de depósito",
                "Una persona deposita S/ 100 en una cuenta de ahorros a plazo fijo libre de comisiones, con una tasa de interés garantizada del 2% anual. Si no realiza ningún depósito adicional ni retira dinero durante ese periodo, ¿cuánto dinero tendrá en la cuenta al finalizar el primer año tras el abono de intereses?",
                "OCDE-INFE - QK5 (Batería SBS/CAF)", 8,
                new Opt("A", "S/ 100", false), new Opt("B", "S/ 102", true), new Opt("C", "S/ 105", false), new Opt("D", "S/ 120", false), new Opt("E", "No lo sé", false));

        seed("PRE_Q05", 5, 3, "Crédito y deuda", "Interés compuesto en endeudamiento",
                "José adquiere un préstamo de S/ 1,000 con una tasa de interés del 20% anual compuesto. Si no realiza ningún amortizo ni pago de cuota, ¿en cuánto tiempo se duplicará la deuda total que mantiene con la entidad financiera?",
                "P-Fin 8 - Q5 (Borrowing / GFLEC)", 9,
                new Opt("A", "En menos de 5 años", true), new Opt("B", "Entre 5 y 10 años", false), new Opt("C", "En más de 10 años", false), new Opt("D", "No lo sé", false));

        seed("PRE_Q06", 6, 3, "Crédito y deuda", "Compra de deuda y costo total",
                "Una persona mantiene una deuda pendiente de S/ 7,400 al 15% anual y paga una cuota mensual de S/ 150. Otra entidad financiera le ofrece comprar su deuda y otorgarle un nuevo crédito por S/ 10,000 al 13% anual manteniendo la misma cuota de S/ 150 al mes. ¿Cuál es la principal desventaja financiera de aceptar esta nueva oferta?",
                "PISA 2012 - PF025Q02 (Nueva oferta)", 12,
                new Opt("A", "La tasa de interés del 13% es más elevada que la tasa original del 15%", false),
                new Opt("B", "Aumentará el capital total adeudado y terminará pagando más intereses totales a lo largo del tiempo", true),
                new Opt("C", "El monto de la cuota mensual de S/ 150 se duplicará de inmediato", false), new Opt("D", "No lo sé", false));

        seed("PRE_Q07", 7, 4, "Pensiones", "Horizonte temporal y riesgo previsional",
                "En el sistema de pensiones (como las AFP), existen distintos tipos de fondo según su nivel de riesgo y horizonte temporal. ¿Cuál de las siguientes decisiones de inversión previsional resulta INAPROPIADA según el ciclo de vida del afiliado?",
                "Adaptación de P-Fin 8 - Q8 / OCDE-INFE - QF8-QF9", 18,
                new Opt("A", "Un joven de 22 años que recién ingresa al mercado laboral y elige un fondo de mayor renta variable para maximizar su rentabilidad a largo plazo", false),
                new Opt("B", "Una persona de 45 años que mantiene un fondo balanceado de riesgo moderado", false),
                new Opt("C", "Un afiliado próximo a jubilarse (64 años) que traslada todos sus ahorros acumulados a un fondo de altísimo riesgo y volatilidad bursátil", true),
                new Opt("D", "No lo sé", false));

        seed("PRE_Q08", 8, 5, "Inversión", "Diversificación de portafolio",
                "Respecto a las alternativas de inversión en el mercado, ¿cuál de las siguientes afirmaciones es correcta?",
                "P-Fin 8 - Q4 (Investing) / OCDE-INFE - QK7_3", 21,
                new Opt("A", "Invertir todo el dinero en acciones de una sola empresa suele ser más seguro que invertir en un fondo mutuo diversificado en múltiples sectores e industrias", false),
                new Opt("B", "Invertir en un fondo mutuo diversificado en múltiples sectores e industrias suele ser más seguro que invertir en las acciones de una sola empresa", true),
                new Opt("C", "Ambas alternativas presentan exactamente el mismo nivel de riesgo", false), new Opt("D", "No lo sé", false));

        seed("PRE_Q09", 9, 5, "Inversión", "Principio fundamental de riesgo y retorno",
                "Si una alternativa o producto financiero ofrece una tasa de rentabilidad o ganancia inusualmente alta, es muy probable que también conlleve un alto riesgo de perder el dinero invertido.",
                "OCDE-INFE - QK7_1 (Batería SBS/CAF)", 20,
                new Opt("A", "Verdadero", true), new Opt("B", "Falso", false), new Opt("C", "No lo sé", false));

        seed("PRE_Q10", 10, 6, "Seguros", "Prioridad actuarial en jóvenes",
                "Camila es una joven trabajadora soltera de 24 años, con buena salud y sin personas a su cargo (sin dependientes). ¿Qué tipo de cobertura de seguro es más prioritaria para ella a corto plazo para proteger su estabilidad financiera?",
                "P-Fin 8 - Q6 (Insuring / GFLEC)", 24,
                new Opt("A", "Un seguro de vida tradicional", false), new Opt("B", "Un seguro de incapacidad laboral o invalidez temporal que proteja la continuidad de sus ingresos ante un accidente o enfermedad", true), new Opt("C", "Un seguro de cuidados de dependencia para la tercera edad", false), new Opt("D", "No lo sé", false));

        seed("PRE_Q11", 11, 6, "Seguros", "Determinación de la prima por riesgo",
                "Mateo contrató un seguro para su vehículo el año pasado. Al momento de renovar su póliza este año, la aseguradora evalúa los cambios ocurridos. ¿Cuál de las siguientes acciones provocará de manera directa un incremento en el costo de la prima (precio a pagar) del seguro?",
                "PISA 2012 - PF002Q01 (Seguro de motocicleta)", 25,
                new Opt("A", "Mateo cambió el color de la pintura de su vehículo", false), new Opt("B", "Mateo provocó dos accidentes de tránsito durante el año anterior", true), new Opt("C", "Mateo cambió la aseguradora a su nombre manteniendo su récord de conductor limpio", false), new Opt("D", "No lo sé", false));

        seed("PRE_Q12", 12, 7, "Sistema financiero y seguridad", "Ciberseguridad bancaria",
                "David recibe un correo electrónico que aparenta provenir de su entidad bancaria, indicando que hubo un fallo en el servidor y solicitándole que ingrese con urgencia a un enlace web adjunto para confirmar su usuario y clave secreta antes de bloquear su cuenta. ¿Qué acción DEBE tomar David ante este mensaje?",
                "PISA 2012 - PF053Q01 (Fallo bancario)", 30,
                new Opt("A", "Responder inmediatamente al correo adjuntando sus credenciales para evitar el bloqueo", false), new Opt("B", "Abrir el enlace adjunto solo si la dirección web se parece al nombre del banco", false), new Opt("C", "No hacer clic en el enlace, desestimar el correo y comunicarse directamente con el banco por sus canales oficiales verificados", true), new Opt("D", "No lo sé", false));
    }

    private void seed(String code, int order, int moduleNumber, String moduleName, String competency,
                      String text, String source, int dktSkillId, Opt... opts) {
        var existing = questionRepository.findByCode(code);
        if (existing.isPresent()) {
            var current = existing.get();
            current.updateDktSkillId(dktSkillId);
            questionRepository.save(current);
            return;
        }
        ExperimentalQuestion q = new ExperimentalQuestion(
                code, order, moduleNumber, moduleName, competency, text, source, dktSkillId
        );
        for (int i = 0; i < opts.length; i++) {
            Opt o = opts[i];
            q.addOption(new ExperimentalOption(code + "_" + o.code(), i + 1, o.text(), o.correct()));
        }
        questionRepository.save(q);
    }

    private record Opt(String code, String text, boolean correct) {}
}
