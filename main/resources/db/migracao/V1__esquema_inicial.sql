-- VendaFácil — esquema inicial.
--
-- Dinheiro é sempre INTEGER de centavos: SQLite não tem DECIMAL e REAL
-- introduziria erro de arredondamento no faturamento.
-- Datas são TEXT em ISO-8601 (YYYY-MM-DDTHH:MM:SS), que ordena
-- lexicograficamente na mesma ordem cronológica.

CREATE TABLE produto (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    nome           TEXT    NOT NULL,
    -- nome em minúsculas, garante unicidade sem diferenciar maiúsculas
    -- (inclusive em acentos, o que COLLATE NOCASE não faz)
    nome_busca     TEXT    NOT NULL,
    preco_centavos INTEGER NOT NULL CHECK (preco_centavos >= 0),
    quantidade     INTEGER NOT NULL CHECK (quantidade >= 0),
    criado_em      TEXT    NOT NULL,
    atualizado_em  TEXT    NOT NULL
);

CREATE UNIQUE INDEX idx_produto_nome_busca ON produto (nome_busca);

CREATE TABLE venda (
    id                      INTEGER PRIMARY KEY AUTOINCREMENT,
    -- ON DELETE SET NULL: excluir um produto não apaga o histórico de vendas
    -- dele, apenas desfaz o vínculo. O nome e o preço abaixo são a cópia
    -- congelada no instante da venda.
    produto_id              INTEGER REFERENCES produto (id) ON DELETE SET NULL,
    nome_produto            TEXT    NOT NULL,
    quantidade              INTEGER NOT NULL CHECK (quantidade > 0),
    preco_unitario_centavos INTEGER NOT NULL CHECK (preco_unitario_centavos >= 0),
    data                    TEXT    NOT NULL
);

CREATE INDEX idx_venda_data ON venda (data);
CREATE INDEX idx_venda_produto ON venda (produto_id);

CREATE TABLE usuario (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    login      TEXT    NOT NULL,
    hash_senha TEXT    NOT NULL,
    salt       TEXT    NOT NULL,
    iteracoes  INTEGER NOT NULL CHECK (iteracoes > 0),
    criado_em  TEXT    NOT NULL
);

CREATE UNIQUE INDEX idx_usuario_login ON usuario (login);
