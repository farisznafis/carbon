import { formatJapaneseAddress, Address } from "@/components/address/formatJapaneseAddress";

type Props = { address: Address };

export function AddressDisplay({ address }: Readonly<Props>) {
  return (
    <p className="whitespace-pre-line text-sm text-muted-foreground">
      {formatJapaneseAddress(address.postalCode, address.prefecture, address.city, address.addressDetail)}
    </p>
  );
}