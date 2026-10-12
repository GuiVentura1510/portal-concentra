package br.org.concentro.portal.missao;

// O que o envio de uma missão pede (V10). Escolhido pelo staff em cada missão.
public enum Comprovacao {
    // Só texto, como antes de 12/10.
    TEXTO,
    // Texto, foto ou os dois; pelo menos um.
    TEXTO_OU_FOTO,
    // Foto obrigatória; o texto é opcional.
    FOTO
}
