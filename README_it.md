Contenuti:

- [1. Weather Report](#1-weather-report)
- [2. Descrizione tecnica](#2-descrizione-tecnica)
  - [2.1 Modello a oggetti](#21-modello-a-oggetti)
  - [2.2 Façade](#22-façade)
  - [2.3 Operations](#23-operations)
  - [2.4 Repository](#24-repository)
  - [2.5 Servizi](#25-servizi)
  - [2.6 Eccezioni](#26-eccezioni)
- [3. Implementazione](#3-implementazione)
  - [3.1 GitFlow e code reviews](#31-gitflow-e-code-reviews)
  - [3.2 Regole dei branch](#32-regole-dei-branch)
  - [3.3 Reportistica](#33-reportistica)
  - [3.4 Test personalizzati](#34-test-personalizzati)
- [4. Dettagli dei requisiti](#4-dettagli-dei-requisiti)
  - [R1 – Network](#r1-network)
  - [R2 – Gateway](#r2-gateway)
  - [R3 – Sensor](#r3-sensor)
  - [R4 – Topology](#r4-topology)

---

# 1. Weather Report

Il sistema **Weather Report** gestisce dati di monitoraggio ambientale raccolti da una infrastruttura composta da **reti**, **gateway** e **sensori**.  
L’obiettivo è memorizzare le misurazioni, configurare regole di controllo, produrre report sui dati raccolti e notificare eventuali rilevazioni fuori soglia.

## Funzionalità generali del sistema

Il sistema Weather Report fornisce funzionalità per la gestione dei dati di monitoraggio e della struttura logica degli elementi.  
Le principali capacità del sistema includono:

- Creazione, modifica e cancellazione di reti, gateway, sensori e dei loro elementi associati (operatori, parametri e soglie).  
  Il sistema registra _chi_ ha creato o modificato una rete, un gateway o un sensore, e _quando_ l’operazione è stata effettuata.  
  Le cancellazioni di Network, Gateway e Sensor devono essere notificate tramite il meccanismo previsto dal sistema.  
  Queste operazioni riguardano l’esistenza degli oggetti nel sistema, indipendentemente da eventuali relazioni gerarchiche.
- Associazione e disassociazione tra elementi (ad esempio collegare un gateway a una rete, o un sensore a un gateway).  
  Le operazioni di associazione appartengono alla gestione della _topologia_ e sono distinte dalle operazioni che riguardano la creazione degli oggetti.
- Importazione e memorizzazione delle misurazioni prodotte dai sensori.
- Analisi delle misurazioni per rilevare valori anomali in base a parametri e soglie configurati.
- Generazione di report a livello di rete, gateway e sensore.
- Notifica delle violazioni di soglia agli operatori associati alle reti coinvolte.

# 2. Descrizione tecnica

Il sistema Weather Report persiste tutti i dati del modello descritto nella sezione precedente su un database relazionale, utilizzando **Hibernate** come JPA provider puro e **H2** come DBMS.

## 2.1 Modello a oggetti

### Classe `Network`

Una _rete di monitoraggio_ che rappresenta un insieme logico di elementi del sistema.  
Può avere un nome e una descrizione, ed è identificata univocamente da un codice.  
Una rete può avere un elenco di _operatori_ responsabili della ricezione delle notifiche.

#### Formato dei codici Network

Il codice di una rete deve essere una stringa che inizia con **"NET\_"** ed è seguita da **due cifre decimali**.

### Classe `Operator`

Un _operatore_ è un soggetto che riceve notifiche quando viene rilevata una violazione di soglia.  
È identificato univocamente dal suo **indirizzo email**.  
Ha nome, cognome e può avere un numero di telefono.  
Lo stesso operatore può essere responsabile di più reti.

---

### Classe `Gateway`

Un _gateway_ raggruppa più dispositivi che monitorano la stessa grandezza fisica.  
Può essere configurato tramite parametri che forniscono informazioni sul suo stato o valori necessari all’interpretazione delle misurazioni.  
Può avere un nome e una descrizione, ed è identificato univocamente da un codice.

#### Formato dei codici Gateway

Il codice di un gateway deve essere una stringa che inizia con **"GW\_"** ed è seguita da **quattro cifre decimali**.

### Classe `Parameter`

Un _parameter_ è un valore associato al gateway a cui appartiene.  
Permette di memorizzare informazioni di stato o configurazione.  
Ogni parametro ha un codice univoco **all’interno del gateway**, e puo avere un nome, una descrizione e un valore numerico.

#### Parametri speciali

Esistono tre codici riservati:

- `EXPECTED_MEAN`
- `EXPECTED_STD_DEV`
- `BATTERY_CHARGE`

Quando presenti, questi parametri vengono utilizzati dal sistema per calcolare parti specifiche del report di un gateway.

---

### Classe `Sensor`

Un _sensore_ misura una grandezza fisica e invia periodicamente le relative misurazioni.  
Può avere un nome, una descrizione ed è identificato univocamente da un codice.  
Un sensore può avere una _soglia_ definita dall’utente per rilevare comportamenti anomali.

#### Formato dei codici Sensor

Il codice di un sensore deve essere una stringa che inizia con **"S\_"** ed è seguita da **sei cifre decimali**.

### Classe `Threshold`

Una _threshold_ definisce un limite accettabile per i valori misurati da un sensore.  
È costituita **sempre** da un valore numerico e da un tipo di confronto che il sistema deve applicare per decidere se una misurazione è anomala.

---

### Classe `Timestamped`

La classe `Timestamped`, estesa da `Network`, `Gateway` e `Sensor` contiene i metadati necessari per tracciare:

- chi ha creato l’istanza;
- quando è stata creata;
- chi l’ha modificata per ultimo;
- quando è stata modificata per l’ultima volta.

### Classe `Measurement`

Una _misurazione_ generata da un sensore, composta **sempre** da:

- il codice della rete, del gateway e del sensore che l’hanno prodotta;
- un timestamp;
- un valore numerico.

I valori vengono letti da file CSV. Questi file sono disponibili in

```
src/main/resources/csv
```

Questi file presentano il seguente header, che definisce la struttura di ogni riga successiva:

```
date, networkCode, gatewayCode, sensorCode, value
```

Ogni riga rappresenta una singola misurazione con il seguente significato:

- date: timestamp della misurazione, espresso usando WeatherReport.DATE_FORMAT
- networkCode: codice della rete associata alla misurazione
- gatewayCode: codice del gateway che ha prodotto la misurazione
- sensorCode: codice del sensore che ha generato la misurazione
- value: valore numerico misurato dal sensore

Tutti i file CSV forniti nella cartella delle risorse seguono questa esatta struttura.

### Classe `User`

Gli _utenti_ del sistema sono **sempre** caratterizzati:

- uno **username** univoco;
- un **tipo**, che determina i permessi dell'utente e che può assumere i valori:
  - `VIEWER`
  - `MAINTAINER`

Le autorizzazioni sono definite come segue:

- Un utente di tipo **VIEWER** può eseguire solo operazioni di _lettura_ (consultazione di dati e report).
- Un utente di tipo **MAINTAINER** può eseguire sia operazioni di _lettura_ sia operazioni di _modifica_ (creazione, aggiornamento, cancellazione di entità e configurazioni).

## 2.2 Façade

La classe `WeatherReport` implementa il concetto di façade, ovvero il **punto di ingresso** principale verso il sistema. È la classe utilizzata dal codice esterno per interagire con le funzionalità esposte.

La façade:

- fornisce metodi di utilità generali, come ad esempio la creazione degli utenti e l’importazione dei dati di misura;
- espone i diversi insiemi di operazioni tramite metodi di accesso dedicati, che restituiscono le interfacce:
  - `NetworkOperations`
  - `GatewayOperations`
  - `SensorOperations`
  - `TopologyOperations`

In questo modo il chiamante lavora con un’unica istanza di `WeatherReport`, delegando alle interfacce delle operations la logica specifica dei singoli ambiti.

## 2.3 Operations

Le **interfacce delle operations** rappresentano i gruppi di funzionalità offerte dal sistema sui diversi tipi di entità:

- `NetworkOperations` per la gestione di reti e operatori;
- `GatewayOperations` per la gestione dei gateway e dei loro parametri;
- `SensorOperations` per la gestione dei sensori e delle soglie;
- `TopologyOperations` per la gestione delle associazioni tra reti, gateway e sensori.

Ogni requisito funzionale (R1, R2, R3, R4) è soddisfatto implementando la relativa interfaccia `*Operations`.  
Le implementazioni concrete contengono la logica applicativa e utilizzano i repository per accedere ai dati persistiti.

### OperationsFactory

`OperationsFactory` è la classe responsabile di fornire istanze concrete delle interfacce delle operations.  
Espone metodi statici come:

- `getNetworkOperations()`
- `getGatewayOperations()`
- `getSensorOperations()`
- `getTopologyOperations()`

che devono restituire le implementazioni reali da utilizzare nel resto del sistema.

La factory è il **punto centrale di configurazione** delle operations, non è l'API principale esposta verso l'esterno: `WeatherReport`, che invece vuole essere l'API esposta verso l'esterno, non si occupa di creare direttamente le implementazioni, ma le ottiene sempre tramite `OperationsFactory`.

## 2.4 Repository

L’accesso al database è incapsulato in classi di **repository**, il cui compito è esporre metodi per leggere e scrivere le entità.  
In questo modo:

- la logica di persistenza è concentrata in un solo livello;
- il resto del codice applicativo non dipende dai dettagli di Hibernate o H2;
- diventa più semplice sostituire o estendere lo strato di persistenza.

### Classe `CRUDRepository`

`CRUDRepository` è una classe generica che implementa le operazioni di base per tutte le entità persistite nel sistema.  
Espone i metodi fondamentali per:

- creare nuove entità;
- leggere singole entità o insiemi di entità;
- aggiornare entità esistenti;
- cancellare entità.

Le operations utilizzano `CRUDRepository` per eseguire le operazioni standard sul database, senza dover duplicare la logica di accesso ai dati.

### Classe `MeasurementRepository`

`MeasurementRepository` è un repository specifico per l’entità `Measurement`.  
La classe estende `CRUDRepository` specificando i tipi generici appropriati, in modo da fornire un punto di accesso centralizzato alle operazioni di persistenza sulle misurazioni.
Questo repository è concepito come il luogo in cui collocare eventuali future operazioni di lettura basate su criteri specifici derivanti dalla struttura dell’entità `Measurement`.  
In questo modo si evita la dispersione di logica di selezione in più parti del sistema, mantenendo un unico punto di estensione per query avanzate.

## 2.5 Servizi

I **servizi** sono classi che espongono metodi riutilizzabili, non legati a una singola entità ma a funzionalità trasversali del sistema.  
Vengono invocati dalle operations o da altre parti del codice quando è necessario eseguire comportamenti specifici, come l’invio di notifiche o l’importazione di dati.

### Servizio di notifica

`AlertingService` è il servizio utilizzato per gestire le notifiche.  
La classe è fornita già implementata e offre due metodi pubblici:

- `public static void notifyThresholdViolation(Collection<Operator> operators, String sensorCode)`  
  utilizzato per notificare agli operatori di una rete il rilevamento di un valore fuori soglia da parte di un sensore;

- `public static void notifyDeletion(String username, String code, Class<?> elementClass)`  
  utilizzato per notificare la cancellazione di un elemento di tipo `Network`, `Gateway` o `Sensor`.  
  Il metodo riceve:
  - lo username dell’utente che esegue la cancellazione;
  - il codice univoco dell’elemento cancellato;
  - la classe dell’elemento, per discriminare il tipo di entità coinvolta.

Questi metodi devono essere invocati nei punti appropriati, in particolare quando vengono rilevate violazioni di soglia dei valori misurati o quando avviene la cancellazione di elementi della gerarchia.

### Servizio di importazione delle misurazioni

`DataImportingService` è il servizio responsabile dell’importazione delle misurazioni da file CSV.

Espone il metodo pubblico:

- `public static void storeMeasurements(String filePath)`

che si occupa di:

- leggere le misurazioni dai file CSV;
- creare le corrispondenti istanze di `Measurement`;
- salvarle nel database tramite i repository.

All’interno della classe è presente il metodo privato:

- `private static void checkMeasurement(Measurement measurement)`

che deve essere chiamato dopo il salvataggio di ciascuna misurazione.  
Questo metodo verifica se il valore appena inserito viola un’eventuale soglia associata al sensore che ha generato la misurazione e, in caso di violazione, deve invocare il metodo appropriato di `AlertingService` per notificare il problema agli operatori interessati.

## 2.6 Eccezioni

Tutte le eccezioni specifiche del sistema estendono la classe `WeatherReportException`, che funge da superclasse comune.  
Le eccezioni descritte non hanno alcuna priorità tra loro: non è rilevante l’ordine con cui potrebbero essere lanciate nel caso un’operazione violi più condizioni contemporaneamente.

### Eccezione `InvalidInputDataException`

Viene lanciata quando vengono forniti dati per attributi obbligatori non validi, mancanti o che non rispettano le convenzioni richieste.  
I valori per i campi opzionali non devono generare questa eccezione.

### Eccezione `IdAlreadyInUseException`

Viene lanciata quando si tenta di creare un nuovo elemento utilizzando un codice univoco già presente nel sistema.

### Eccezione `ElementNotFoundException`

Viene lanciata quando si fornisce il codice di un elemento che il sistema non contiene.

### Eccezione `UnauthorizedException`

Viene lanciata quando lo username passato all’operazione:

- non corrisponde ad alcun utente esistente, oppure
- corrisponde a un utente che non ha i permessi necessari per eseguire l’operazione.

# 3. Implementazione

L’implementazione del sistema è organizzata in quattro requisiti distinti:

- tre requisiti individuali (R1, R2, R3), tra loro indipendenti;
- un requisito di integrazione (R4), che si basa sui precedenti.

I requisiti R1, R2 e R3 devono essere sviluppati in modo autonomo, ognuno da un singolo membro del team.

Per ciascuno dei requisiti individuali (R1, R2, R3) le funzionalità da implementare si articolano in due blocchi principali:

- gestione degli elementi del sistema rilevanti per il requisito (creazione, aggiornamento, cancellazione, lettura);
- reportistica, ovvero calcolo di valori aggregati e statistiche sulle misurazioni.

## 3.1 GitFlow e code reviews

Durante lo sviluppo del progetto, tutti i membri del team devono seguire il workflow
GitFlow come descritto dalla documentazione:

- Specifica GitFlow:  
  https://git-oop.polito.it/labs/docs/-/blob/main/Git/GitFlow_it.md

Ogni funzionalità deve essere sviluppata in un branch dedicato, rispettando
le regole del workflow riportate nel documento. Tutte le integrazioni nel branch
`main` devono avvenire esclusivamente tramite Merge Request.

Le code review sono obbligatorie: ogni Merge Request deve essere valutata
utilizzando la checklist fornita all’interno del repository del progetto:

- [Checklist per la Code Review](./checklist.md)

I membri del team devono applicare tutti i punti della checklist durante la revisione
del lavoro dei colleghi prima di approvare una Merge Request.

## 3.2 Regole dei branch

Ciascun requisito deve essere implementato su un branch dedicato, il cui nome deve **iniziare** con un prefisso del seguente formato:

- `X-rN`

dove:

- `X` è un numero intero qualsiasi;
- `N` è l’indice del requisito (`1`, `2`, `3` oppure `4`).
  Dopo questo prefisso il nome del branch può contenere qualsiasi ulteriore suffisso (ad esempio `1-r2-network`, `3-r1-feature-x`): il sistema di test considera solo la parte iniziale che corrisponde al prefisso `X-rN`.

Questo schema permette al sistema di test di riconoscere a quale requisito si riferisce il branch e di eseguire la suite di test appropriata.  
Nei branch relativi ai requisiti individuali (R1, R2, R3) sono ammessi solo commit del membro incaricato di quel requisito.

Il requisito R4 è un requisito di integrazione: viene affrontato dopo che le implementazioni dei requisiti individuali sono state integrate nel branch `main`.  
In questo contesto è possibile effettuare refactoring, uniformare le scelte progettuali e sviluppare funzionalità che dipendono direttamente dai risultati dei requisiti R1, R2 e R3.  
Sul branch dedicato a R4 sono ammessi commit da parte di tutti i membri del team.

## 3.3 Reportistica

Il report fornito in output dalle funzioni deputate a calcolarlo si basa sui parametri `startDate` ed `endDate`. Questi parametri:

- non considerano la timezone, ovvero sono espressi come date assolute,
- sono opzionali (possono essere _null_),
- sono nel formato `DATE_FORMAT` definito in `WeatherReport`,
- delimitano l’intervallo temporale inclusivo delle misurazioni da considerare,
- se il valore è nullo, l’intervallo non viene limitato in quella direzione (ad esempio: `startDate = null` significa che non esiste un limite inferiore; `endDate = null` significa che non esiste un limite superiore).

Le parti di reportistica previste nei vari requisiti richiedono il calcolo di un istogramma specifico (basato sul tipo corrente di report) e di alcuni valori statistici di base sulle misurazioni:

- media;
- varianza campionaria;
- deviazione standard;
- identificazione degli outlier.

Se il numero di misurazioni disponibili è minore di 2, i valori di varianza e deviazione standard non sono significativi e devono essere impostati a `0` e l'eventuale insieme degli outliers risulta vuoto.

### Media

Dato un insieme di \(n\) misurazioni la media è definita come:

$$
\overline{x} = \frac{1}{n} \sum_{i=1}^{n} x_i
$$

### Varianza

Si utilizza la varianza campionaria, adatta a insiemi finiti di misurazioni:

$$
\sigma^2 = \frac{1}{n - 1} \sum_{i=1}^{n} (x_i - \overline{x})^2
$$

### Deviazione standard

La deviazione standard è definita come:

$$
\sigma = \sqrt{\sigma^2}
$$

### Outlier

Una misurazione $$ x_i $$ è considerata un outlier se si discosta dalla media di almeno due volte la deviazione standard:

$$
\left| x_i - \overline{x} \right| \ge 2 \sigma
$$

### Semantica dei range negli istogrammi

Tutti gli istogrammi del sistema Weather Report sono rappresentati tramite
chiavi di tipo `Report.Range<T>`. Se non diversamente specificato:

- i range sono **chiusi a sinistra e aperti a destra** (`[start, end)`): un valore `v`
  appartiene a un bucket se `start ≤ v < end`;
- l’**ultimo bucket** di ogni istogramma è **chiuso a destra e sinistra** (`[start, end]` ovvero `start ≤ v ≤ end`), in modo che il
  valore massimo osservato nell’intervallo sia sempre incluso in qualche
  bucket.

Il tipo concreto usato come parametro di `Range<T>` dipende dal report
(`LocalDateTime`, `Duration` oppure `Double`).

## 3.4 Test personalizzati

È possibile implementare test personalizzati aggiuntivi, purché vengano inseriti **obbligatoriamente** nel package `com.weather.report.test.custom`. In questo modo tali test possono essere esclusi in modo programmato dalla valutazione automatica del progetto, evitando interferenze con il processo di grading.  
La cartella contiene un file `.gitkeep` vuoto, perché, essendo vuota nella versione iniziale del repository, Git non la includerebbe. Il file `.gitkeep` serve solo a forzare il versionamento della directory, rendendola disponibile per l’inserimento dei test personalizzati.

# 4. Dettagli dei requisiti

## R1 Network

Il requisito R1 riguarda la gestione delle entità `Network` e `Operator`, insieme a una parte di reportistica a livello di rete.

### Importazione dati

Per questo requisito è necessario completare l’implementazione del metodo `storeMeasurements` nella classe `DataImportingService`:

```
    public static void storeMeasurements(String filePath)
```

Il metodo deve:

- leggere le misurazioni dai file CSV;
- creare le corrispondenti istanze di `Measurement` e salvarle nel database;
- dopo ogni salvataggio, invocare il metodo privato:

```
    private static void checkMeasurement(Measurement measurement)
```

#### Controllo violazioni soglie

Nel metodo `checkMeasurement(Measurement measurement)` occorre implementare il controllo delle eventuali soglie associate al sensore che ha prodotto la misurazione.

In particolare, la logica deve:

- ottenere il sensore corrispondente alla misurazione;
- verificare se per quel sensore è stata definita una soglia (`Threshold`);
- stabilire se il valore misurato viola la soglia configurata;
- in caso di violazione, invocare il metodo:

```
    AlertingService.notifyThresholdViolation(...)
```

come descritto nella [sezione 2.5](#25-servizi) sui servizi di notifica.

In questo punto esisterebbe, in teoria, una dipendenza diretta dall’implementazione di `Sensor` e `Threshold`.
Questa dipendenza viene però eliminata dal fatto che, nei test, l’interazione con `CRUDRepository<Sensor, String>` è _mockata_: il mocking consiste nel sostituire l’implementazione reale con un oggetto di prova che simula il comportamento atteso e restituisce dati controllati.
Perché questo meccanismo funzioni correttamente, è importante utilizzare il sensore referenziato dalla variabile `currentSensor` già presente nel metodo, senza modificare la struttura del codice fornito ma aggiungendo a questo la logica operativa per il controllo del valore salvato.

### NetworkOperations

L’interfaccia `NetworkOperations` raggruppa le funzionalità di gestione delle reti e degli operatori, oltre alla produzione del report di rete.
I metodi esposti hanno nomi esplicativi; per comprendere quando lanciare le varie eccezioni è sufficiente fare riferimento alla [sezione 2.6](#26-eccezioni) dedicata al modello delle eccezioni.

L’implementazione concreta di `NetworkOperations` deve:

- creare, aggiornare e cancellare le entità `Network` e `Operator`;
- utilizzare i metadati ereditati da `Timestamped` per registrare informazioni di creazione e modifica delle reti;
- notificare la cancellazione di una `Network` tramite la chiamata:

```
    AlertingService.notifyDeletion(...)
```

Il metodo

```
Collection<Network> getNetworks(String... codes)
```

permette di ottenere tutti gli oggetti Network il cui codice è passato nell'elenco di parametri del metodo. Se un codice passato in input non corrisponde ad un elemento presente nel sistema, viene semplicemente ignorato. Nel caso in cui il metodo venga invocato senza nessun parametro di input, il metodo deve ritornare tutti gli elementi Network presenti nel sistema.

#### NetworkReport

`NetworkOperations` espone anche il metodo per ottenere un `NetworkReport` a partire dal codice di una rete e da un intervallo temporale opzionale.

Il `NetworkReport` deve contenere:

- `networkCode`: il codice passato in input
- `startDate` / `endDate`: le stringhe ricevute (anche nulle)
- `numberOfMeasurements`: numero totale di misurazioni della rete nell’intervallo
- `mostActiveGateways`: gateway con il maggior numero di misurazioni
- `leastActiveGateways`: gateway con il minor numero di misurazioni
- `gatewaysLoadRatio`: mappa `<gatewayCode, ratio>`
  - `ratio` è la percentuale di misurazioni generate dal singolo gateway rispetto al totale della rete
- `histogram`: mappa `<Range<LocalDateTime>, count>`.  
   La mappa raggruppa le misurazioni della rete in sotto-intervalli temporali consecutivi
  (bucket), la cui granularità può essere oraria o giornaliera a seconda della durata
  dell’intervallo richiesto o, se assente, dell’intervallo effettivo delle misurazioni
  disponibili.  
  Ogni chiave `Range<LocalDateTime>` contiene gli istanti esatti di inizio e fine del bucket
  e la relativa unità (`HOUR` oppure `DAY`).  
  I bucket seguono la convenzione globale sugli istogrammi: sono chiusi a sinistra
  e aperti a destra `[start, end)`, ad eccezione dell’ultimo bucket che è
  `[start, end]` in modo che il timestamp massimo risulti incluso.  
  Il valore associato è il numero di misurazioni i cui timestamp ricadono nel
  bucket secondo questa convenzione (`start ≤ t < end`, oppure `start ≤ t ≤ end`
  per l’ultimo bucket).
  L'istogramma è rappresentato da una `SortedMap`: i bucket coprono interamente l’intervallo
  considerato e sono restituiti in ordine crescente rispetto al loro istante di inizio.

### OperationsFactory

Per completare il requisito R1 è necessario:

- fornire una implementazione concreta di `NetworkOperations`;
- aggiornare la classe `OperationsFactory` affinché il metodo:
  ```
    public static NetworkOperations getNetworkOperations()
  ```
  restituisca un’istanza della propria implementazione.

## R2 Gateway

Il requisito R2 si concentra sulla gestione dei `Gateway` e dei relativi `Parameter`, oltre alla reportistica a livello di gateway.

### Importazione dati

Per R2 è richiesto l’utilizzo del metodo `storeMeasurements` di `DataImportingService`:

```
    public static void storeMeasurements(String filePath)
```

L’implementazione deve:

- leggere le misurazioni dal file CSV;
- salvare nel database tramite i repository;
- invocare, dopo ogni salvataggio, il metodo:
  ```
      checkMeasurement(measurement)
  ```
  anche se il contenuto di `checkMeasurement` non è implementato nel branch relativo a R2.

### GatewayOperations

L’interfaccia `GatewayOperations` raggruppa i metodi che permettono di:

- creare, aggiornare e cancellare gateway;
  - la cancellazione di un Gateway comporta anche la cancellazione di tutti i suoi parametri;
- definire e modificare i parametri associati ai gateway;
- produrre il report di gateway.

L’implementazione concreta dovrà:

- rispettare le condizioni di lancio delle eccezioni;
- usare i metadati di `Timestamped` per tracciare la creazione e le modifiche dei gateway;
- invocare `AlertingService.notifyDeletion(...)` quando un gateway viene eliminato.

Il metodo

```
Collection<Gateway> getGateways(String... codes)
```

permette di ottenere tutti gli oggetti Gateway il cui codice è passato nell'elenco di parametri del metodo. Se un codice passato in input non corrisponde ad un elemento presente nel sistema, questo viene semplicemente ignorato. Nel caso in cui il metodo venga invocato senza nessun parametro di input, il metodo deve ritornare tutti gli elementi Gateway presenti nel sistema.

#### GatewayReport

Il `GatewayReport` deve contenere:

- `code`: il codice del Gateway richiesto
- `startDate` / `endDate`: le stringhe ricevute in input (anche nulle)
- `numberOfMeasurements`: numero totale di misurazioni del Gateway nell’intervallo richiesto
- `mostActiveSensors`: sensori con il maggior numero di misurazioni
- `leastActiveSensors`: sensori con il minor numero di misurazioni
- `sensorsLoadRatio`: mappa `<sensorCode, ratio>`
  - `ratio` è la percentuale di misurazioni effettuate dal singolo sensore rispetto al totale del Gateway
- `outlierSensors`: elenco dei `sensorCode` la cui media dei valori rilevati risulta anomala secondo la formula sotto riportata
- `batteryChargePercentage`: il valore corrente del Parameter `BATTERY_CHARGE` del Gateway (non dipende dall'intervallo di tempo richiesto)
- `histogram`: mappa `<Range<Duration>, count>`.  
   Rappresenta l’istogramma dei tempi di inter-arrivo tra misurazioni consecutive
  del gateway nell’intervallo richiesto.  
   Se esistono almeno due misurazioni, il sistema calcola tutte le differenze
  temporali tra misurazioni consecutive e suddivide il range risultante in
  **20 intervalli contigui**, rappresentati da `Range<Duration>`.  
  Ogni chiave identifica un intervallo di durate che segue la convenzione
  globale sugli istogrammi: tutti i bucket tranne l’ultimo sono chiusi a
  sinistra e aperti a destra `[start, end)`, mentre l’ultimo bucket è
  `[start, end]` in modo che il tempo di inter-arrivo massimo risulti incluso.  
  Il valore associato è il numero di tempi di inter-arrivo che ricadono in
  quell’intervallo.
  I bucket coprono interamente l’intervallo `[minDuration, maxDuration]` e la
  mappa è una `SortedMap` ordinata rispetto al tempo di inizio del bucket:
  iterando sulla mappa si ottengono i bucket in ordine crescente di tempo di
  inter-arrivo.

---

### Calcolo degli Outlier Sensors

Un sensore è considerato _outlier_ se la sua **media delle misurazioni** si discosta dal valore atteso (`EXPECTED_MEAN`) di **almeno due volte la deviazione standard attesa** (`EXPECTED_STD_DEV`).

La condizione formale è la seguente:

$$
\left| \overline{x}_{\text{sensor}} - \mu_{\text{expected}} \right| \ge 2 \cdot \sigma_{\text{expected}}
$$

Dove:

- $\overline{x}_{\text{sensor}}$ è la media delle misurazioni del sensore nell’intervallo temporale richiesto
- $\mu_{\text{expected}}$ è il valore del parametro `EXPECTED_MEAN` del Gateway
- $\sigma_{\text{expected}}$ è il valore del parametro `EXPECTED_STD_DEV` del Gateway

### OperationsFactory

Per completare il requisito R2 occorre:

- sviluppare una implementazione concreta di `GatewayOperations`;
- modificare `OperationsFactory` in modo che il metodo:
  ```
      public static GatewayOperations getGatewayOperations()
  ```
  restituisca l’istanza della relativa implementazione.

---

## R3 Sensor

Il requisito R3 riguarda la gestione dei `Sensor` e delle `Threshold`, insieme al report a livello di singolo sensore.

### Importazione dati

Anche per R3 è necessario utilizzare il metodo `storeMeasurements` di `DataImportingService`:

```
    public static void storeMeasurements(String filePath)
```

L’implementazione deve:

- leggere le misurazioni dai file CSV;
- salvarle nel database utilizzando i repository;
- chiamare `checkMeasurement(measurement)` dopo ogni inserimento.

È sufficiente che la chiamata a `checkMeasurement` sia presente nel punto corretto, anche se la logica interna del metodo non è implementata all’interno del branch dedicato a R3.

### SensorOperations

L’interfaccia `SensorOperations` contiene i metodi per:

- creare, aggiornare e cancellare sensori;
  - la cancellazione di un Sensor comporta anche la cancellazione della eventuale soglia associata;
- gestire le soglie associate ai sensori;
- recuperare sensori a partire dai loro codici;
- ottenere il report per un sensore specifico.

L’implementazione deve:

- applicare le regole di validazione e di lancio delle eccezioni descritte nella [sezione 2.6](#26-eccezioni);
- utilizzare `Timestamped` per registrare le informazioni di creazione e modifica dei sensori;
- invocare `AlertingService.notifyDeletion(...)` quando un sensore viene cancellato.

Il metodo

```
Collection<Sensor> getSensors(String... codes)
```

permette di ottenere tutti gli oggetti Sensor il cui codice è passato nell'elenco di parametri del metodo. Se un codice passato in input non corrisponde ad un elemento presente nel sistema, questo viene semplicemente ignorato. Nel caso in cui il metodo venga invocato senza nessun parametro di input, il metodo deve ritornare tutti gli elementi Sensor presenti nel sistema.

#### SensorReport

Il `SensorReport` contiene le seguenti informazioni:

- `code`: codice del Sensor richiesto,
- `startDate`, `endDate`: le date ricevute in input (possono essere null),
- `numberOfMeasurements`: numero totale di misurazioni rilevate nell’intervallo,
- `mean`: media dei valori misurati,
- `variance`: varianza delle misurazioni,
- `stdDev`: deviazione standard,
- `minimumMeasuredValue`: valore minimo rilevato,
- `maximumMeasuredValue`: valore massimo rilevato,
- `outliers`: misurazioni considerate outlier.
- `histogram`: mappa `<Range<Double>, count>`.  
   La mappa rappresenta un istogramma dei valori misurati dal sensore
  nell’intervallo considerato, costruito utilizzando **solo** le misurazioni
  che non sono classificate come outlier.  
   Il sistema determina il valore minimo e massimo tra le misurazioni non
  outlier e suddivide questo intervallo in **20 sotto-intervalli di uguale
  ampiezza**, ognuno rappresentato da un `Range<Double>` i cui estremi sono inclusivo a sinistra ed esclusivo a destra, tranne l’ultimo intervallo che è `[start, end]` in modo che il valore massimo osservato risulti incluso.  
  Il valore associato a ogni intervallo è il numero di misurazioni non outlier che ricadono in quell’intervallo secondo questa convenzione (`start ≤ v < end`, oppure `start ≤ v ≤ end` per l’ultimo intervallo).
  La stessa strategia di suddivisione (20 bin equi-ampiezza sullo span
  [min, max] dei valori non outlier) deve essere applicata in modo coerente
  a tutti i sensori, mentre i limiti effettivi dei bin dipendono dai valori
  reali osservati per il sensore.  
  La mappa è una `SortedMap`: i bucket sono ordinati in base al valore di inizio
  dell’intervallo, per cui l’iterazione sulla mappa restituisce i bucket in ordine crescente di
  valore misurato. Se non esistono misurazioni non outlier nell’intervallo del report,
  l’istogramma può risultare vuoto.

### OperationsFactory

Per soddisfare il requisito R3 è necessario:

- implementare concretamente l’interfaccia `SensorOperations`;
- aggiornare `OperationsFactory` affinché il metodo:
  ```
      public static SensorOperations getSensorOperations()
  ```
  restituisca l’istanza corretta.

---

## R4 Topology

Il requisito R4 è un requisito di integrazione e riguarda la gestione della topologia del sistema, ovvero delle relazioni tra `Network`, `Gateway` e `Sensor`.

Il branch dedicato a R4 deve essere creato a partire da `main`, dopo che le implementazioni dei requisiti R1, R2 e R3 sono state integrate.
Su questo branch è possibile:

- effettuare refactoring del codice;
- uniformare le scelte di progettazione;
- completare le funzionalità che richiedono la collaborazione tra le diverse parti del sistema.

### Refactoring

In questa fase è possibile:

- eliminare duplicazioni;
- riorganizzare il codice in modo più chiaro;
- centralizzare logiche comuni introdotte nei requisiti individuali;
- migliorare la leggibilità e la manutenibilità complessiva del progetto.

Il refactoring deve preservare le interfacce pubbliche e il corretto funzionamento dei test esistenti.

### TopologyOperations

L’interfaccia `TopologyOperations` definisce le operazioni legate alle relazioni tra le entità principali, ad esempio:

- associare o disassociare un gateway a una rete;
- associare o disassociare un sensore a un gateway;
- ottenere l’elenco dei gateway associati a una rete;
- ottenere l’elenco dei sensori associati a un gateway.

L’implementazione concreta di questa interfaccia deve applicare le stesse regole di validazione e gestione delle eccezioni descritte in precedenza.

### OperationsFactory

Per completare il requisito R4 è infine necessario:

- fornire una implementazione di `TopologyOperations`;
- aggiornare `OperationsFactory` in modo che il metodo:
  ```
      public static TopologyOperations getTopologyOperations()
  ```
  restituisca la relativa implementazione.

In questo modo l’oggetto `WeatherReport` potrà esporre anche le funzionalità di gestione della topologia, integrando in un unico punto di accesso tutte le operazioni sviluppate nei requisiti R1, R2, R3 e R4.
