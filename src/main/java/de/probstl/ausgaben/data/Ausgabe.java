package de.probstl.ausgaben.data;

import javax.validation.constraints.NotNull;

public class Ausgabe {

	@NotNull(message="Geschaeft muss angegeben werden!")
	private String m_Geschaeft;
	private String m_Beschreibung;
	@NotNull(message="Betrag muss angegeben werden!")
	private Double m_Betrag;

	public Ausgabe(String geschaeft, String beschreibung, Double betrag) {
		m_Geschaeft = geschaeft;
		m_Beschreibung = beschreibung;
		m_Betrag = betrag;
	}

	public String getGeschaeft() {
		return m_Geschaeft;
	}

	public void setGeschaeft(String m_Geschaeft) {
		this.m_Geschaeft = m_Geschaeft;
	}

	public String getBeschreibung() {
		return m_Beschreibung;
	}

	public void setBeschreibung(String m_Beschreibung) {
		this.m_Beschreibung = m_Beschreibung;
	}

	public Double getBetrag() {
		return m_Betrag;
	}

	public void setBetrag(Double m_Betrag) {
		this.m_Betrag = m_Betrag;
	}
}
