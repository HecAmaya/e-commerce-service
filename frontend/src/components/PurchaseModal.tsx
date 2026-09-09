type Props = {
  quantity: number
  onQuantityChange: (quantity: number) => void
  onConfirm: () => void
  onCancel: () => void
}

export default function PurchaseModal({ quantity, onQuantityChange, onConfirm, onCancel }: Props) {
  return <div className="modal-backdrop"><div className="modal"><h2>Confirm purchase</h2><p>How many units would you like to purchase?</p>
    <input min="1" type="number" value={quantity} onChange={e => onQuantityChange(Number(e.target.value))} />
    <div className="form-actions"><button className="primary" onClick={onConfirm}>Confirm order</button><button onClick={onCancel}>Cancel</button></div>
  </div></div>
}
