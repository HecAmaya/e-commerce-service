import type { Product } from '../services/api'

type Props = {
  product: Product
  onEdit: (product: Product) => void
  onDelete: (id: number) => void
  onPurchase: (product: Product) => void
}

export default function ProductCard({ product, onEdit, onDelete, onPurchase }: Props) {
  return <article className="card">
    <div className="card-top"><span className="category">{product.category}</span><span className={product.stock > 0 ? 'in-stock' : 'out-stock'}>{product.stock > 0 ? `${product.stock} in stock` : 'Out of stock'}</span></div>
    <h3>{product.name}</h3><p className="sku">{product.sku}</p><p className="description">{product.description}</p>
    <div className="card-bottom"><strong>${Number(product.price).toFixed(2)}</strong><span>{Number(product.weightKg).toFixed(3)} kg</span></div>
    <div className="actions">
      <button onClick={() => onEdit(product)}>Edit</button>
      <button onClick={() => onDelete(product.id)}>Delete</button>
      <button className="primary" disabled={!product.stock} onClick={() => onPurchase(product)}>Purchase</button>
    </div>
  </article>
}
