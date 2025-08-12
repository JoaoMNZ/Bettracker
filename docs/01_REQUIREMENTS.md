# Bettracker Requirements

This document details the functional and non-functional requirements for the Bettracker project.

## Functional Requirements

Functional requirements describe the system's features and functions.

### FR - Bettor Account Management
* **FR01:** The system must allow bettors to register.
* **FR02:** The system must send a verification code to the bettor's email upon registration to confirm ownership.
* **FR03:** The system must allow a bettor to recover their password via email.
* **FR04:** The system must allow a bettor to modify their personal information.

### FR - Bet Management
* **FR05:** The system must allow a bettor to create, edit, and delete their own custom lists of bookmakers, tipsters, sports, and competitions.
* **FR06:** The system must allow a bettor to set their betting unit value.
* **FR07:** The system must allow a bettor to log a bet containing the following fields: Title, Selection, Bookmaker, Tipster, Sport, Competition, Stake, Stake type(as a value or unit percentage), Odds, Status (Pending, Won, Lost, Push, Half Won, Half Lost, Cashout, and Void), and Date/Time.
* **FR08:** The system must allow a bettor to log deposit and withdrawal transactions for each bookmaker.

### FR - Data Visualization & Analysis
* **FR09:** The system must provide a section to display all registered bets, allowing the bettor to edit, delete, and filter them.
* **FR10:** The system must provide a dashboard with essential statistics such as ROI, profit, and win rate.

---

## Non-Functional Requirements (NFR)

Non-functional requirements define the quality attributes and operational criteria of the system.

* **NFR01:** Bettor session management must be stateless and implemented using JSON Web Tokens (JWT).
* **NFR02:** Sensitive data, such as passwords, must be securely hashed using the BCrypt algorithm.
* **NFR03:** The bet list in the main betting section must be paginated to optimize performance.
* **NFR04:** In the bet registration form, the date and time fields must be pre-filled with the current values.
* **NFR05:** In the bet registration form, the Competition field must be dynamically filtered after a Sport is selected.
