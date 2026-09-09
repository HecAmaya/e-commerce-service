type Props = {
  productName: string
  availableStock: number
  quantity: number
  onQuantityChange: (quantity: number) => void
  onConfirm: () => void
  onCancel: () => void
}

export default function PurchaseModal({ productName, availableStock, quantity, onQuantityChange, onConfirm, onCancel }: Props) {
  return <div className="modal-backdrop"><div className="modal"><h2>Confirm purchase</h2><p>How many units would you like to purchase?</p>
    <p><strong>{productName}</strong> · {availableStock} available</p>
    <input min="1" max={availableStock} type="number" value={quantity} onChange={e => onQuantityChange(Math.min(availableStock, Math.max(1, Number(e.target.value) || 1)))} />
    <div className="form-actions"><button className="primary" disabled={availableStock < 1} onClick={onConfirm}>Confirm order</button><button onClick={onCancel}>Cancel</button></div>
  </div></div>
}
