package com.tradepilot.dto.ai;

public class TradeSnapshot {
    private Long id;
    private String symbol;
    private String side;
    private Double pnl;
    private Integer quantity;
    private Double entryPrice;
    private Double exitPrice;
    private String entryTime;
    private String exitTime;
    private String session;
    private String setup;
    private String notes;

    @java.lang.SuppressWarnings("all")
    public TradeSnapshot() {
    }

    @java.lang.SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    @java.lang.SuppressWarnings("all")
    public String getSymbol() {
        return this.symbol;
    }

    @java.lang.SuppressWarnings("all")
    public String getSide() {
        return this.side;
    }

    @java.lang.SuppressWarnings("all")
    public Double getPnl() {
        return this.pnl;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getQuantity() {
        return this.quantity;
    }

    @java.lang.SuppressWarnings("all")
    public Double getEntryPrice() {
        return this.entryPrice;
    }

    @java.lang.SuppressWarnings("all")
    public Double getExitPrice() {
        return this.exitPrice;
    }

    @java.lang.SuppressWarnings("all")
    public String getEntryTime() {
        return this.entryTime;
    }

    @java.lang.SuppressWarnings("all")
    public String getExitTime() {
        return this.exitTime;
    }

    @java.lang.SuppressWarnings("all")
    public String getSession() {
        return this.session;
    }

    @java.lang.SuppressWarnings("all")
    public String getSetup() {
        return this.setup;
    }

    @java.lang.SuppressWarnings("all")
    public String getNotes() {
        return this.notes;
    }

    @java.lang.SuppressWarnings("all")
    public void setId(final Long id) {
        this.id = id;
    }

    @java.lang.SuppressWarnings("all")
    public void setSymbol(final String symbol) {
        this.symbol = symbol;
    }

    @java.lang.SuppressWarnings("all")
    public void setSide(final String side) {
        this.side = side;
    }

    @java.lang.SuppressWarnings("all")
    public void setPnl(final Double pnl) {
        this.pnl = pnl;
    }

    @java.lang.SuppressWarnings("all")
    public void setQuantity(final Integer quantity) {
        this.quantity = quantity;
    }

    @java.lang.SuppressWarnings("all")
    public void setEntryPrice(final Double entryPrice) {
        this.entryPrice = entryPrice;
    }

    @java.lang.SuppressWarnings("all")
    public void setExitPrice(final Double exitPrice) {
        this.exitPrice = exitPrice;
    }

    @java.lang.SuppressWarnings("all")
    public void setEntryTime(final String entryTime) {
        this.entryTime = entryTime;
    }

    @java.lang.SuppressWarnings("all")
    public void setExitTime(final String exitTime) {
        this.exitTime = exitTime;
    }

    @java.lang.SuppressWarnings("all")
    public void setSession(final String session) {
        this.session = session;
    }

    @java.lang.SuppressWarnings("all")
    public void setSetup(final String setup) {
        this.setup = setup;
    }

    @java.lang.SuppressWarnings("all")
    public void setNotes(final String notes) {
        this.notes = notes;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof TradeSnapshot)) return false;
        final TradeSnapshot other = (TradeSnapshot) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$pnl = this.getPnl();
        final java.lang.Object other$pnl = other.getPnl();
        if (this$pnl == null ? other$pnl != null : !this$pnl.equals(other$pnl)) return false;
        final java.lang.Object this$quantity = this.getQuantity();
        final java.lang.Object other$quantity = other.getQuantity();
        if (this$quantity == null ? other$quantity != null : !this$quantity.equals(other$quantity)) return false;
        final java.lang.Object this$entryPrice = this.getEntryPrice();
        final java.lang.Object other$entryPrice = other.getEntryPrice();
        if (this$entryPrice == null ? other$entryPrice != null : !this$entryPrice.equals(other$entryPrice)) return false;
        final java.lang.Object this$exitPrice = this.getExitPrice();
        final java.lang.Object other$exitPrice = other.getExitPrice();
        if (this$exitPrice == null ? other$exitPrice != null : !this$exitPrice.equals(other$exitPrice)) return false;
        final java.lang.Object this$symbol = this.getSymbol();
        final java.lang.Object other$symbol = other.getSymbol();
        if (this$symbol == null ? other$symbol != null : !this$symbol.equals(other$symbol)) return false;
        final java.lang.Object this$side = this.getSide();
        final java.lang.Object other$side = other.getSide();
        if (this$side == null ? other$side != null : !this$side.equals(other$side)) return false;
        final java.lang.Object this$entryTime = this.getEntryTime();
        final java.lang.Object other$entryTime = other.getEntryTime();
        if (this$entryTime == null ? other$entryTime != null : !this$entryTime.equals(other$entryTime)) return false;
        final java.lang.Object this$exitTime = this.getExitTime();
        final java.lang.Object other$exitTime = other.getExitTime();
        if (this$exitTime == null ? other$exitTime != null : !this$exitTime.equals(other$exitTime)) return false;
        final java.lang.Object this$session = this.getSession();
        final java.lang.Object other$session = other.getSession();
        if (this$session == null ? other$session != null : !this$session.equals(other$session)) return false;
        final java.lang.Object this$setup = this.getSetup();
        final java.lang.Object other$setup = other.getSetup();
        if (this$setup == null ? other$setup != null : !this$setup.equals(other$setup)) return false;
        final java.lang.Object this$notes = this.getNotes();
        final java.lang.Object other$notes = other.getNotes();
        if (this$notes == null ? other$notes != null : !this$notes.equals(other$notes)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof TradeSnapshot;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $pnl = this.getPnl();
        result = result * PRIME + ($pnl == null ? 43 : $pnl.hashCode());
        final java.lang.Object $quantity = this.getQuantity();
        result = result * PRIME + ($quantity == null ? 43 : $quantity.hashCode());
        final java.lang.Object $entryPrice = this.getEntryPrice();
        result = result * PRIME + ($entryPrice == null ? 43 : $entryPrice.hashCode());
        final java.lang.Object $exitPrice = this.getExitPrice();
        result = result * PRIME + ($exitPrice == null ? 43 : $exitPrice.hashCode());
        final java.lang.Object $symbol = this.getSymbol();
        result = result * PRIME + ($symbol == null ? 43 : $symbol.hashCode());
        final java.lang.Object $side = this.getSide();
        result = result * PRIME + ($side == null ? 43 : $side.hashCode());
        final java.lang.Object $entryTime = this.getEntryTime();
        result = result * PRIME + ($entryTime == null ? 43 : $entryTime.hashCode());
        final java.lang.Object $exitTime = this.getExitTime();
        result = result * PRIME + ($exitTime == null ? 43 : $exitTime.hashCode());
        final java.lang.Object $session = this.getSession();
        result = result * PRIME + ($session == null ? 43 : $session.hashCode());
        final java.lang.Object $setup = this.getSetup();
        result = result * PRIME + ($setup == null ? 43 : $setup.hashCode());
        final java.lang.Object $notes = this.getNotes();
        result = result * PRIME + ($notes == null ? 43 : $notes.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "TradeSnapshot(id=" + this.getId() + ", symbol=" + this.getSymbol() + ", side=" + this.getSide() + ", pnl=" + this.getPnl() + ", quantity=" + this.getQuantity() + ", entryPrice=" + this.getEntryPrice() + ", exitPrice=" + this.getExitPrice() + ", entryTime=" + this.getEntryTime() + ", exitTime=" + this.getExitTime() + ", session=" + this.getSession() + ", setup=" + this.getSetup() + ", notes=" + this.getNotes() + ")";
    }
}
