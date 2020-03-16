package de.probstl.ausgaben.data;

import java.util.Date;

import javax.validation.constraints.NotNull;

public class Ausgabe {

	@NotNull(message="Geschaeft muss angegeben werden!")
	private String m_Geschaeft;

	@NotNull(message="Beschreibung muss angegeben werden!")
	private String m_Beschreibung;
	
	@NotNull(message="Betrag muss angegeben werden!")
	private Double m_Betrag;

	private Date m_Zeitpunkt;
	
	public Ausgabe(String geschaeft, String beschreibung, Double betrag, Date date) {
		m_Geschaeft = geschaeft;
		m_Beschreibung = beschreibung;
		m_Betrag = betrag;
		m_Zeitpunkt = date;
	}

	public String getGeschaeft() {
		return m_Geschaeft;
	}

	public void setGeschaeft(String geschaeft) {
		m_Geschaeft = geschaeft;
	}

	public String getBeschreibung() {
		return m_Beschreibung;
	}

	public void setBeschreibung(String beschreibung) {
		m_Beschreibung = beschreibung;
	}

	public Double getBetrag() {
		return m_Betrag;
	}

	public void setBetrag(Double betrag) {
		m_Betrag = betrag;
	}

	public Date getZeitpunkt() {
		return m_Zeitpunkt;
	}

	public void setZeitpunkt(Date zeitpunkt) {
		m_Zeitpunkt = zeitpunkt;
	}
	
}
