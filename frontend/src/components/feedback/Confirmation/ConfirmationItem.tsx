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
            <div className="text-sm font-medium text-muted-foreground mb-1">
                {label}
            </div>
            <div
                className={
                    isChanged
                        ? "underline underline-offset-4 font-medium"
                        : "text-foreground"
                }
            >
                {String(value)}
            </div>
        </div>
    );
};
