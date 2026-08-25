import { ChangedConfirmationValue } from './ChangedConfirmationValue';

interface ConfirmationItemProps<T> {
    label: string;
    value: T;
    originalValue?: T;
}

export const ConfirmationItem = <T,>({
    label,
    value,
    originalValue,
}: ConfirmationItemProps<T>) => {
    const isChanged =
        originalValue !== undefined && originalValue !== value;

    return (
        <div>
            <div className="text-sm font-medium text-foreground mb-1">
                {label}
            </div>
            <div className={isChanged ? undefined : "text-foreground font-bold"}>
                {isChanged ? (
                    <ChangedConfirmationValue>
                        {String(value)}
                    </ChangedConfirmationValue>
                ) : (
                    String(value)
                )}
            </div>
        </div>
    );
};
